package ru.jobhunter.infrastructure.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class QuestionnaireResumeEvidenceCandidateExtractor {

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "(?U)[\\p{L}\\p{Nd}][\\p{L}\\p{Nd}+#.\\-/]*"
    );

    private static final Pattern PROTECTED_TOKEN_PATTERN = Pattern.compile(
            "(?U)(?:[A-ZА-Я][\\p{L}\\p{Nd}+#.\\-/]*"
                    + "|\\p{Nd}[\\p{L}\\p{Nd}+#.\\-/]*)"
    );

    private static final int MAX_CANDIDATES = 6;
    private static final int MAX_FRAGMENT_CHARS = 700;
    private static final int MAX_DIRECT_EVIDENCE_CHARS = 240;

    private static final Set<String> GENERIC_TERMS = Set.of(
            "как", "какой", "какая", "какие", "каком", "какого",
            "ли", "у", "вас", "есть", "был", "была", "были",
            "опыт", "работы", "работал", "работать",
            "уровень", "владения", "знаний", "навык", "навыки",
            "с", "со", "в", "во", "на", "по", "для", "и", "или",
            "а", "но", "что", "это", "этот", "эта", "эти",
            "технология", "технологии", "инструмент", "инструменты",
            "библиотека", "библиотеки", "фреймворк", "фреймворки",
            "разработка", "разработки", "тестирование",
            "тестирования", "использование", "применение"
    );

    String extract(
            String questionText,
            String resumeText,
            int maxChars
    ) {
        String normalizedQuestion = requireText(
                questionText,
                "Question text must not be blank"
        );
        String normalizedResume = requireText(
                resumeText,
                "Resume text must not be blank"
        );

        if (maxChars < 1) {
            throw new IllegalArgumentException(
                    "Maximum evidence length must be positive"
            );
        }

        List<String> fragments = splitIntoFragments(normalizedResume);
        Set<String> questionTerms = meaningfulTokens(normalizedQuestion);

        if (questionTerms.isEmpty()) {
            return limit(normalizedResume, maxChars);
        }

        List<ScoredFragment> scoredFragments = scoreFragments(
                questionTerms,
                fragments
        );

        StringBuilder result = new StringBuilder();
        int selectedCount = 0;

        Optional<DirectResumeEvidenceCandidate> directEvidence =
                findDirectEvidence(
                        normalizedQuestion,
                        normalizedResume
                );

        if (directEvidence.isPresent()) {
            appendFragment(
                    result,
                    directEvidence.get().quote()
            );
            selectedCount++;
        }

        for (ScoredFragment scoredFragment : scoredFragments) {
            if (selectedCount >= MAX_CANDIDATES) {
                break;
            }

            String fragment = scoredFragment.text();

            if (result.toString().contains(fragment)) {
                continue;
            }

            if (result.length() + fragment.length() + 2 > maxChars) {
                continue;
            }

            appendFragment(result, fragment);
            selectedCount++;
        }

        return result.isEmpty()
                ? limit(normalizedResume, maxChars)
                : result.toString();
    }

    Optional<DirectResumeEvidenceCandidate> findDirectEvidence(
            String questionText,
            String resumeText
    ) {
        String normalizedQuestion = requireText(
                questionText,
                "Question text must not be blank"
        );
        String normalizedResume = requireText(
                resumeText,
                "Resume text must not be blank"
        );

        List<NamedTerm> requiredTerms = extractNamedTerms(
                normalizedQuestion
        );

        /*
         * Автоматическое серверное подтверждение допустимо только
         * для именованных сущностей: Java, PostgreSQL, Kafka,
         * Selenium, Docker, JUnit и т. п.
         *
         * Общие формулировки вроде «базы данных» не подтверждаем
         * автоматически: для них остаётся LLM + review flow.
         */
        if (requiredTerms.isEmpty()) {
            return Optional.empty();
        }

        List<ScoredDirectCandidate> candidates = new ArrayList<>();
        List<String> fragments = splitIntoFragments(normalizedResume);

        for (int index = 0; index < fragments.size(); index++) {
            String fragment = fragments.get(index);

            Optional<String> quote = extractDirectQuote(
                    fragment,
                    requiredTerms
            );

            if (quote.isEmpty()) {
                continue;
            }

            int score = calculateDirectEvidenceScore(
                    requiredTerms,
                    fragment
            );

            candidates.add(
                    new ScoredDirectCandidate(
                            quote.get(),
                            score,
                            index
                    )
            );
        }

        return candidates.stream()
                .sorted(
                        Comparator.comparingInt(
                                ScoredDirectCandidate::score
                        ).reversed().thenComparingInt(
                                ScoredDirectCandidate::originalIndex
                        )
                )
                .findFirst()
                .map(candidate ->
                        new DirectResumeEvidenceCandidate(
                                buildTopic(requiredTerms),
                                candidate.quote(),
                                requiredTerms.stream()
                                        .map(NamedTerm::normalized)
                                        .collect(
                                                java.util.stream.Collectors
                                                        .toUnmodifiableSet()
                                        )
                        )
                );
    }

    private List<ScoredFragment> scoreFragments(
            Set<String> questionTerms,
            List<String> fragments
    ) {
        List<ScoredFragment> scoredFragments = new ArrayList<>();

        for (int index = 0; index < fragments.size(); index++) {
            String fragment = fragments.get(index);
            int score = calculateRelevance(questionTerms, fragment);

            if (score > 0) {
                scoredFragments.add(
                        new ScoredFragment(
                                fragment,
                                score,
                                index
                        )
                );
            }
        }

        scoredFragments.sort(
                Comparator.comparingInt(
                        ScoredFragment::score
                ).reversed().thenComparingInt(
                        ScoredFragment::originalIndex
                )
        );

        return List.copyOf(scoredFragments);
    }

    private Optional<String> extractDirectQuote(
            String fragment,
            List<NamedTerm> requiredTerms
    ) {
        String compactFragment = compact(fragment);

        if (!containsAllTerms(compactFragment, requiredTerms)) {
            return Optional.empty();
        }

        if (compactFragment.length() <= MAX_DIRECT_EVIDENCE_CHARS) {
            return Optional.of(compactFragment);
        }

        String shortened = shortenAroundFirstNamedTerm(
                compactFragment,
                requiredTerms
        );

        if (!containsAllTerms(shortened, requiredTerms)) {
            return Optional.empty();
        }

        return Optional.of(shortened);
    }

    private boolean containsAllTerms(
            String text,
            List<NamedTerm> requiredTerms
    ) {
        Set<String> textTerms = meaningfulTokens(text);

        for (NamedTerm requiredTerm : requiredTerms) {
            boolean matched = textTerms.stream()
                    .anyMatch(textTerm ->
                            matchesNamedTerm(
                                    textTerm,
                                    requiredTerm.normalized()
                            )
                    );

            if (!matched) {
                return false;
            }
        }

        return true;
    }

    private boolean matchesNamedTerm(
            String textTerm,
            String requiredTerm
    ) {
        return textTerm.equals(requiredTerm)
                || textTerm.startsWith(requiredTerm + "-")
                || textTerm.startsWith(requiredTerm + "/")
                || textTerm.startsWith(requiredTerm + ".")
                || textTerm.endsWith("-" + requiredTerm)
                || textTerm.endsWith("/" + requiredTerm)
                || textTerm.endsWith("." + requiredTerm);
    }

    private String shortenAroundFirstNamedTerm(
            String text,
            List<NamedTerm> requiredTerms
    ) {
        String lowerCaseText = text.toLowerCase(Locale.ROOT);

        int firstMatchIndex = requiredTerms.stream()
                .mapToInt(term -> {
                    int index = lowerCaseText.indexOf(
                            term.normalized()
                    );

                    return index < 0 ? Integer.MAX_VALUE : index;
                })
                .min()
                .orElse(0);

        if (firstMatchIndex == Integer.MAX_VALUE) {
            return text.substring(
                    0,
                    MAX_DIRECT_EVIDENCE_CHARS
            ).strip();
        }

        int start = Math.max(
                0,
                firstMatchIndex - MAX_DIRECT_EVIDENCE_CHARS / 3
        );
        int end = Math.min(
                text.length(),
                start + MAX_DIRECT_EVIDENCE_CHARS
        );

        start = moveStartToWordBoundary(text, start);
        end = moveEndToWordBoundary(text, end);

        return compact(text.substring(start, end));
    }

    private int moveStartToWordBoundary(
            String value,
            int index
    ) {
        int cursor = index;

        while (cursor > 0
                && !Character.isWhitespace(
                value.charAt(cursor - 1)
        )) {
            cursor--;
        }

        return cursor;
    }

    private int moveEndToWordBoundary(
            String value,
            int index
    ) {
        int cursor = index;

        while (cursor < value.length()
                && !Character.isWhitespace(
                value.charAt(cursor)
        )) {
            cursor++;
        }

        return cursor;
    }

    private int calculateDirectEvidenceScore(
            List<NamedTerm> requiredTerms,
            String fragment
    ) {
        Set<String> fragmentTerms = meaningfulTokens(fragment);

        int score = 0;

        for (NamedTerm requiredTerm : requiredTerms) {
            boolean matched = fragmentTerms.stream()
                    .anyMatch(fragmentTerm ->
                            matchesNamedTerm(
                                    fragmentTerm,
                                    requiredTerm.normalized()
                            )
                    );

            if (matched) {
                score += 10;
            }
        }

        return score + Math.min(fragment.length(), 300) / 100;
    }

    private int calculateRelevance(
            Set<String> questionTerms,
            String fragment
    ) {
        Set<String> fragmentTerms = meaningfulTokens(fragment);
        int score = 0;

        for (String questionTerm : questionTerms) {
            boolean matched = fragmentTerms.stream()
                    .anyMatch(fragmentTerm ->
                            matchesNamedTerm(
                                    fragmentTerm,
                                    questionTerm
                            )
                    );

            if (matched) {
                score++;
            }
        }

        return score;
    }

    private List<NamedTerm> extractNamedTerms(String value) {
        LinkedHashMap<String, String> terms = new LinkedHashMap<>();

        Matcher matcher = PROTECTED_TOKEN_PATTERN.matcher(
                Normalizer.normalize(
                        value,
                        Normalizer.Form.NFKC
                )
        );

        while (matcher.find()) {
            String original = matcher.group().trim();
            String normalized = normalizeToken(original);

            if (isMeaningfulToken(normalized)) {
                terms.putIfAbsent(normalized, original);
            }
        }

        return terms.entrySet().stream()
                .map(entry -> new NamedTerm(
                        entry.getValue(),
                        entry.getKey()
                ))
                .toList();
    }

    private String buildTopic(List<NamedTerm> terms) {
        return terms.stream()
                .map(NamedTerm::original)
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private List<String> splitIntoFragments(String resumeText) {
        LinkedHashSet<String> fragments = new LinkedHashSet<>();

        for (String line : resumeText.split("\\R+")) {
            String compactLine = compact(line);

            if (compactLine.isBlank()) {
                continue;
            }

            if (compactLine.length() <= MAX_FRAGMENT_CHARS) {
                fragments.add(compactLine);
                continue;
            }

            for (String sentence : compactLine.split(
                    "(?<=[.!?])\\s+"
            )) {
                String compactSentence = compact(sentence);

                if (!compactSentence.isBlank()) {
                    fragments.add(compactSentence);
                }
            }
        }

        if (fragments.isEmpty()) {
            fragments.add(compact(resumeText));
        }

        return List.copyOf(fragments);
    }

    private Set<String> meaningfulTokens(String value) {
        Set<String> result = new LinkedHashSet<>();

        Matcher matcher = TOKEN_PATTERN.matcher(
                Normalizer.normalize(
                        value,
                        Normalizer.Form.NFKC
                )
        );

        while (matcher.find()) {
            String token = normalizeToken(matcher.group());

            if (isMeaningfulToken(token)) {
                result.add(token);
            }
        }

        return Set.copyOf(result);
    }

    private boolean isMeaningfulToken(String token) {
        if (token.isBlank() || GENERIC_TERMS.contains(token)) {
            return false;
        }

        if (token.length() >= 3) {
            return true;
        }

        return token.chars().anyMatch(Character::isDigit)
                || token.chars().anyMatch(
                character -> !Character.isLetterOrDigit(character)
        );
    }

    private void appendFragment(
            StringBuilder target,
            String fragment
    ) {
        if (!target.isEmpty()) {
            target.append("\n\n");
        }

        target.append(fragment);
    }

    private String limit(String value, int maxChars) {
        if (value.length() <= maxChars) {
            return value;
        }

        return value.substring(0, maxChars).strip();
    }

    private String requireText(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.strip();
    }

    private String normalizeToken(String value) {
        return Normalizer.normalize(
                        value,
                        Normalizer.Form.NFKC
                ).toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .trim();
    }

    private String compact(String value) {
        return value == null
                ? ""
                : value.replaceAll("\\s+", " ").trim();
    }

    private record NamedTerm(
            String original,
            String normalized
    ) {
    }

    private record ScoredFragment(
            String text,
            int score,
            int originalIndex
    ) {
    }

    private record ScoredDirectCandidate(
            String quote,
            int score,
            int originalIndex
    ) {
    }
}

record DirectResumeEvidenceCandidate(
        String topic,
        String quote,
        Set<String> matchedTerms
) {

    DirectResumeEvidenceCandidate {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException(
                    "Direct evidence topic must not be blank"
            );
        }

        if (quote == null || quote.isBlank()) {
            throw new IllegalArgumentException(
                    "Direct evidence quote must not be blank"
            );
        }

        matchedTerms = Set.copyOf(matchedTerms);
    }
}