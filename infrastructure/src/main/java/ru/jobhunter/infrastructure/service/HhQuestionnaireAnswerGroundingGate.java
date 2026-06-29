package ru.jobhunter.infrastructure.service;

import org.jspecify.annotations.NonNull;
import ru.jobhunter.core.application.dto.CandidateQuestionnaireProfileDto;
import ru.jobhunter.core.application.dto.GenerateHhQuestionnaireAnswersCommand;
import ru.jobhunter.core.application.dto.GeneratedHhQuestionnaireAnswerDto;
import ru.jobhunter.core.application.dto.HhQuestionnaireAnswerQuality;
import ru.jobhunter.core.application.dto.HhQuestionnaireQuestionDto;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.jobhunter.core.domain.model.CandidateSalaryTaxBasis;

final class HhQuestionnaireAnswerGroundingGate {

    private static final Logger log = LoggerFactory.getLogger(
            HhQuestionnaireAnswerGroundingGate.class
    );

    private static final int MAX_EVIDENCE_LENGTH = 240;

    private static final QuestionnaireResumeEvidenceCandidateExtractor
            DIRECT_RESUME_EVIDENCE_CANDIDATE_EXTRACTOR =
            new QuestionnaireResumeEvidenceCandidateExtractor();

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "(?U)[\\p{L}\\p{Nd}][\\p{L}\\p{Nd}+#.\\-/]*"
    );

    private static final Pattern PROTECTED_TOKEN_PATTERN = Pattern.compile(
            "(?U)(?:[A-ZА-Я][\\p{L}\\p{Nd}+#.\\-/]*"
                    + "|\\p{Nd}[\\p{L}\\p{Nd}+#.\\-/]*)"
    );

    private static final Pattern SKILL_LEVEL_QUESTION_PATTERN =
            Pattern.compile(
                    "(?iu)^\\s*(?:как\\s+оцениваете\\s+)?"
                            + "(?:свой\\s+)?уровень\\s+владения\\s+"
                            + "(.+?)[?!.]*\\s*$"
            );

    private static final Pattern EXPERIENCE_QUESTION_PATTERN =
            Pattern.compile(
                    "(?iu)^\\s*(?:был|есть)\\s+ли\\s+у\\s+вас\\s+"
                            + "опыт\\s+работы\\s+с\\s+"
                            + "(.+?)[?!.]*\\s*$"
            );

    private static final Set<String> GENERIC_TERMS = Set.of(
            "как", "какой", "какая", "какие", "каком", "какого",
            "какому", "ли", "у", "вас", "есть", "был", "была",
            "были", "имеется", "имеют", "опыт", "работы",
            "работал", "работала", "работали", "работать",
            "с", "со", "в", "во", "на", "по", "для", "и", "или",
            "а", "но", "что", "это", "этот", "эта", "эти",
            "мой", "моя", "мое", "мои", "уровень", "владения",
            "знаний", "знаком", "знакома", "знакомы", "готов",
            "готова", "готовы", "быстро", "под", "задачи",
            "проекта", "проект", "использование", "использовал",
            "использовала", "использовали", "применение",
            "применял", "применяла", "применяли", "навык",
            "навыки", "технология", "технологии", "инструмент",
            "инструменты", "библиотека", "библиотеки",
            "фреймворк", "фреймворки", "разработка",
            "разработки", "тестирование", "тестирования",
            "архитектура", "архитектуры", "система", "системы",
            "платформа", "платформы", "да", "нет"
    );

    boolean isStructuredProfileQuestion(
            HhQuestionnaireQuestionDto question
    ) {
        Objects.requireNonNull(
                question,
                "Questionnaire question must not be null"
        );

        String normalizedQuestion = normalizeForSearch(
                question.questionText()
        );

        return isSalaryQuestion(normalizedQuestion)
                || isTestAssignmentQuestion(normalizedQuestion)
                || isRelocationQuestion(normalizedQuestion)
                || isEnglishQuestion(normalizedQuestion)
                || isBusinessTripsQuestion(normalizedQuestion)
                || isStartAvailabilityQuestion(normalizedQuestion)
                || isWorkFormatQuestion(normalizedQuestion)
                || isTimeZoneQuestion(normalizedQuestion);
    }

    boolean canResolveFromStructuredProfile(
            HhQuestionnaireQuestionDto question,
            Optional<CandidateQuestionnaireProfileDto> optionalProfile
    ) {
        Objects.requireNonNull(
                question,
                "Questionnaire question must not be null"
        );
        Objects.requireNonNull(
                optionalProfile,
                "Candidate profile optional must not be null"
        );

        return resolveStructuredProfileFact(
                question.questionText(),
                optionalProfile
        ).isPresent();
    }

    List<GeneratedHhQuestionnaireAnswerDto> apply(
            GenerateHhQuestionnaireAnswersCommand command,
            Optional<CandidateQuestionnaireProfileDto> optionalProfile,
            List<HhQuestionnaireAnswerProposal> proposals
    ) {
        Objects.requireNonNull(command, "Questionnaire command must not be null");
        Objects.requireNonNull(optionalProfile, "Candidate profile optional must not be null");
        Objects.requireNonNull(proposals, "Questionnaire proposals must not be null");

        Map<String, HhQuestionnaireAnswerProposal> proposalsByField = getStringHhQuestionnaireAnswerProposalMap(proposals);

        CandidateEvidenceSources evidenceSources =
                CandidateEvidenceSources.from(command, optionalProfile);

        List<GeneratedHhQuestionnaireAnswerDto> result = new ArrayList<>();

        for (HhQuestionnaireQuestionDto question : command.questions()) {
            HhQuestionnaireAnswerProposal proposal =
                    proposalsByField.get(question.fieldName());

            if (proposal == null) {
                throw new IllegalStateException(
                        "LLM proposal was not found for field: "
                                + question.fieldName()
                );
            }

            Optional<ProfileFactResolution> profileFact =
                    resolveStructuredProfileFact(
                            question.questionText(),
                            optionalProfile
                    );

            if (profileFact.isPresent()) {
                ProfileFactResolution resolved = profileFact.get();

                result.add(new GeneratedHhQuestionnaireAnswerDto(
                        question.fieldName(),
                        resolved.answer(),
                        HhQuestionnaireAnswerQuality.CONFIRMED,
                        "",
                        List.of(resolved.evidence())
                ));

                continue;
            }

            if (isStructuredProfileQuestion(question)) {
                result.add(
                        buildStructuredProfileReviewRequiredAnswer(
                                question,
                                optionalProfile
                        )
                );
                continue;
            }

            Optional<GroundedEvidence> groundedEvidence =
                    resolveGroundedEvidence(
                            question,
                            proposal,
                            evidenceSources
                    );

            if (groundedEvidence.isEmpty()) {
                groundedEvidence = resolveDirectResumeEvidence(
                        question,
                        evidenceSources
                );
            }

            if (groundedEvidence.isPresent()) {
                GroundedEvidence evidence = groundedEvidence.get();

                result.add(new GeneratedHhQuestionnaireAnswerDto(
                        question.fieldName(),
                        selectConfirmedAnswer(
                                proposal.answer(),
                                evidence.quote()
                        ),
                        HhQuestionnaireAnswerQuality.CONFIRMED,
                        "",
                        List.of(
                                evidence.source().name()
                                        + ":"
                                        + limit(evidence.quote())
                        )
                ));

                continue;
            }

            result.add(buildReviewRequiredAnswer(
                    question,
                    optionalProfile
            ));
        }

        return List.copyOf(result);
    }

    private static @NonNull Map<String, HhQuestionnaireAnswerProposal>
    getStringHhQuestionnaireAnswerProposalMap(List<HhQuestionnaireAnswerProposal> proposals) {
        Map<String, HhQuestionnaireAnswerProposal> proposalsByField =
                new HashMap<>();

        for (HhQuestionnaireAnswerProposal proposal : proposals) {
            HhQuestionnaireAnswerProposal previous = proposalsByField.put(
                    proposal.fieldName(),
                    proposal
            );

            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate questionnaire proposal for field: "
                                + proposal.fieldName()
                );
            }
        }
        return proposalsByField;
    }

    private Optional<ProfileFactResolution> resolveStructuredProfileFact(
            String questionText,
            Optional<CandidateQuestionnaireProfileDto> optionalProfile
    ) {
        if (optionalProfile.isEmpty()) {
            return Optional.empty();
        }

        CandidateQuestionnaireProfileDto profile =
                optionalProfile.get();

        String question = normalizeForSearch(questionText);

        if (isSalaryQuestion(question)) {
            return resolveSalaryProfileFact(question, profile);
        }

        if (isTestAssignmentQuestion(question)) {
            return resolveTestAssignmentProfileFact(profile);
        }

        if (isRelocationQuestion(question)) {
            return Optional.of(new ProfileFactResolution(
                    profile.relocationReady()
                            ? "Готов к переезду."
                            : "К переезду не готов.",
                    "PROFILE:Переезд "
                            + (profile.relocationReady()
                            ? "разрешён"
                            : "не рассматривается")
            ));
        }

        if (isEnglishQuestion(question)) {
            return Optional.of(new ProfileFactResolution(
                    "Уровень английского языка — "
                            + profile.englishLevel()
                            + ".",
                    "PROFILE:Английский "
                            + profile.englishLevel()
            ));
        }

        if (isBusinessTripsQuestion(question)) {
            return Optional.of(new ProfileFactResolution(
                    profile.businessTripsReady()
                            ? "Готов к командировкам."
                            : "Командировки не рассматриваю.",
                    "PROFILE:Командировки "
                            + (profile.businessTripsReady()
                            ? "разрешены"
                            : "не рассматриваются")
            ));
        }

        if (isStartAvailabilityQuestion(question)) {
            return Optional.of(new ProfileFactResolution(
                    formatStartAvailability(profile),
                    "PROFILE:Готовность начать работу "
                            + profile.startAvailability().name()
            ));
        }

        if (isWorkFormatQuestion(question)) {
            return Optional.of(new ProfileFactResolution(
                    formatWorkPreference(profile),
                    "PROFILE:Формат работы "
                            + profile.workFormatPreference().name()
            ));
        }

        if (isTimeZoneQuestion(question)) {
            return Optional.of(new ProfileFactResolution(
                    "Мой часовой пояс — "
                            + profile.timeZoneId()
                            + ".",
                    "PROFILE:Часовой пояс "
                            + profile.timeZoneId()
            ));
        }

        return Optional.empty();
    }

    private Optional<ProfileFactResolution> resolveSalaryProfileFact(
            String question,
            CandidateQuestionnaireProfileDto profile
    ) {
        Optional<CandidateSalaryTaxBasis> requestedTaxBasis =
                requestedSalaryTaxBasis(question);

        if (requestedTaxBasis.isPresent()
                && profile.salaryTaxBasis()
                != requestedTaxBasis.get()) {
            return Optional.empty();
        }

        String salarySuffix = switch (profile.salaryTaxBasis()) {
            case NET -> " на руки";
            case GROSS -> " до вычета налогов";
            case UNSPECIFIED -> "";
        };

        String answer;

        if (asksMinimumSalary(question)) {
            answer = "Рассматриваю предложения от "
                    + formatAmount(profile.salaryMin())
                    + " "
                    + profile.salaryCurrency()
                    + salarySuffix
                    + " в месяц.";
        } else {
            answer = "Рассматриваю зарплату в диапазоне "
                    + formatAmount(profile.salaryMin())
                    + "–"
                    + formatAmount(profile.salaryMax())
                    + " "
                    + profile.salaryCurrency()
                    + salarySuffix
                    + " в месяц.";
        }

        return Optional.of(new ProfileFactResolution(
                answer,
                "PROFILE:Зарплатные ожидания "
                        + formatAmount(profile.salaryMin())
                        + "–"
                        + formatAmount(profile.salaryMax())
                        + " "
                        + profile.salaryCurrency()
                        + salarySuffix
        ));
    }

    private Optional<ProfileFactResolution>
    resolveTestAssignmentProfileFact(
            CandidateQuestionnaireProfileDto profile
    ) {
        return switch (profile.testAssignmentReadiness()) {
            case YES -> Optional.of(new ProfileFactResolution(
                    "Да, готов выполнить тестовое задание.",
                    "PROFILE:Тестовое задание разрешено"
            ));

            case NO -> Optional.of(new ProfileFactResolution(
                    "Нет, выполнение тестового задания не рассматриваю.",
                    "PROFILE:Тестовое задание не рассматривается"
            ));

            case UNKNOWN -> Optional.empty();
        };
    }

    private boolean isSalaryQuestion(String question) {
        return containsAnyFragment(
                question,
                "зарплат",
                "заработн",
                "оклад",
                "доход",
                "вознагражден"
        );
    }

    private boolean isTestAssignmentQuestion(String question) {
        return containsAnyFragment(
                question,
                "тестовое задание",
                "тестов задан",
                "выполнить тестов"
        );
    }

    private boolean isRelocationQuestion(String question) {
        return containsAnyFragment(question, "переезд", "релокац");
    }

    private boolean isEnglishQuestion(String question) {
        return containsAnyFragment(
                question,
                "английск",
                "english",
                "иностранн язык"
        );
    }

    private boolean isBusinessTripsQuestion(String question) {
        return containsAnyFragment(
                question,
                "командировк",
                "бизнес поездк"
        );
    }

    private boolean isStartAvailabilityQuestion(String question) {
        return containsAnyFragment(
                question,
                "когда готовы начать",
                "когда сможете начать",
                "дата выхода",
                "приступить к работе",
                "начать работу"
        );
    }

    private boolean isWorkFormatQuestion(String question) {
        return containsAnyFragment(
                question,
                "формат работы",
                "удален",
                "гибрид",
                "офис"
        );
    }

    private boolean isTimeZoneQuestion(String question) {
        return containsAnyFragment(
                question,
                "часовой пояс",
                "timezone",
                "time zone"
        );
    }

    private Optional<CandidateSalaryTaxBasis> requestedSalaryTaxBasis(
            String question
    ) {
        if (containsAnyFragment(
                question,
                "на руки",
                "чистыми",
                "после вычета",
                " net"
        )) {
            return Optional.of(CandidateSalaryTaxBasis.NET);
        }

        if (containsAnyFragment(
                question,
                "до вычета",
                "до налог",
                "грязными",
                " gross"
        )) {
            return Optional.of(CandidateSalaryTaxBasis.GROSS);
        }

        return Optional.empty();
    }

    private boolean asksMinimumSalary(String question) {
        return containsAnyFragment(
                question,
                "от какой суммы",
                "от какого дохода",
                "от какой зарплаты",
                "минимальн",
                "не менее",
                "начиная с какой"
        );
    }

    private Optional<GroundedEvidence> resolveDirectResumeEvidence(
            HhQuestionnaireQuestionDto question,
            CandidateEvidenceSources evidenceSources
    ) {
        if (!isProfessionalCapabilityQuestion(
                question.questionText()
        )) {
            return Optional.empty();
        }

        return DIRECT_RESUME_EVIDENCE_CANDIDATE_EXTRACTOR
                .findDirectEvidence(
                        question.questionText(),
                        evidenceSources.resumeText()
                )
                .map(candidate -> {
                    log.debug(
                            "HH questionnaire direct resume evidence selected: "
                                    + "fieldName={}, topic={}, "
                                    + "quoteLength={}, matchedTerms={}",
                            question.fieldName(),
                            candidate.topic(),
                            candidate.quote().length(),
                            candidate.matchedTerms()
                    );

                    return new GroundedEvidence(
                            HhQuestionnaireEvidenceSource.RESUME,
                            candidate.quote()
                    );
                });
    }

    private Optional<GroundedEvidence> resolveGroundedEvidence(
            HhQuestionnaireQuestionDto question,
            HhQuestionnaireAnswerProposal proposal,
            CandidateEvidenceSources sources
    ) {
        HhQuestionnaireEvidenceSource source =
                HhQuestionnaireEvidenceSource.from(
                        proposal.evidenceSource()
                );

        if (source == HhQuestionnaireEvidenceSource.NONE) {
            logGroundingRejected(
                    question,
                    proposal,
                    source,
                    "evidence-source-none"
            );
            return Optional.empty();
        }

        String sourceText = switch (source) {
            case RESUME -> sources.resumeText();
            case PROFILE_ADDITIONAL -> sources.additionalFacts();
            case NONE -> "";
        };

        String evidenceQuote = compact(proposal.evidenceQuote());

        if (evidenceQuote.isBlank()) {
            logGroundingRejected(
                    question,
                    proposal,
                    source,
                    "evidence-quote-blank"
            );
            return Optional.empty();
        }

        if (!containsNormalizedText(sourceText, evidenceQuote)) {
            logGroundingRejected(
                    question,
                    proposal,
                    source,
                    "evidence-quote-not-found"
            );
            return Optional.empty();
        }

        if (!isEvidenceSubstantial(evidenceQuote)) {
            logGroundingRejected(
                    question,
                    proposal,
                    source,
                    "evidence-quote-not-substantial"
            );
            return Optional.empty();
        }

        if (!isTopicGrounded(
                question.questionText(),
                proposal.topic(),
                evidenceQuote
        )) {
            logGroundingRejected(
                    question,
                    proposal,
                    source,
                    "topic-not-grounded"
            );
            return Optional.empty();
        }

        log.debug(
                "HH questionnaire grounding accepted: "
                        + "fieldName={}, evidenceSource={}, topic={}, "
                        + "evidenceQuoteLength={}",
                question.fieldName(),
                source.name(),
                compact(proposal.topic()),
                evidenceQuote.length()
        );

        return Optional.of(new GroundedEvidence(source, evidenceQuote));
    }

    private void logGroundingRejected(
            HhQuestionnaireQuestionDto question,
            HhQuestionnaireAnswerProposal proposal,
            HhQuestionnaireEvidenceSource source,
            String reason
    ) {
        log.debug(
                "HH questionnaire grounding rejected: "
                        + "fieldName={}, reason={}, evidenceSource={}, "
                        + "topic={}, evidenceQuoteLength={}",
                question.fieldName(),
                reason,
                source.name(),
                compact(proposal.topic()),
                compact(proposal.evidenceQuote()).length()
        );
    }

    private boolean isTopicGrounded(
            String questionText,
            String topic,
            String evidenceQuote
    ) {
        Set<String> topicTerms = meaningfulTokens(topic);
        Set<String> evidenceTerms = meaningfulTokens(evidenceQuote);

        if (topicTerms.isEmpty()
                || !intersects(topicTerms, evidenceTerms)) {
            return false;
        }

        Set<String> questionTerms = meaningfulTokens(questionText);

        /*
         * Для общих вопросов вроде «Какие технологии использовали?»
         * questionTerms будут пустыми после удаления общих слов.
         * В таком случае достаточно подтверждения topic в evidenceQuote.
         */
        return questionTerms.isEmpty()
                || intersects(questionTerms, topicTerms);
    }

    private boolean isEvidenceSubstantial(String evidenceQuote) {
        return compact(evidenceQuote).length() >= 3
                && !meaningfulTokens(evidenceQuote).isEmpty();
    }

    private String selectConfirmedAnswer(
            String proposedAnswer,
            String evidenceQuote
    ) {
        String answer = compact(proposedAnswer);

        if (answer.isBlank()) {
            return evidenceQuote;
        }

        if (isAnswerCompatibleWithEvidence(answer, evidenceQuote)) {
            return answer;
        }

        /*
         * Модель предложила формулировку с фактами, которые не удалось
         * подтвердить цитатой. В этом случае используем сам подтверждённый
         * фрагмент резюме/профиля, а не неподтверждённый текст модели.
         */
        return evidenceQuote;
    }

    private boolean isAnswerCompatibleWithEvidence(
            String answer,
            String evidenceQuote
    ) {
        Set<String> answerTerms = meaningfulTokens(answer);
        Set<String> evidenceTerms = meaningfulTokens(evidenceQuote);

        if (answerTerms.isEmpty()) {
            return false;
        }

        long overlapCount = answerTerms.stream()
                .filter(evidenceTerms::contains)
                .count();

        double overlapRatio = (double) overlapCount / answerTerms.size();

        if (overlapRatio < 0.45) {
            return false;
        }

        Set<String> protectedAnswerTokens = protectedTokens(answer);
        Set<String> protectedEvidenceTokens =
                protectedTokens(evidenceQuote);

        return protectedEvidenceTokens.containsAll(
                protectedAnswerTokens
        );
    }

    private GeneratedHhQuestionnaireAnswerDto buildReviewRequiredAnswer(
            HhQuestionnaireQuestionDto question,
            Optional<CandidateQuestionnaireProfileDto> optionalProfile
    ) {
        boolean draftsAllowed = optionalProfile
                .map(
                        CandidateQuestionnaireProfileDto
                                ::allowRelatedExperienceDrafts
                )
                .orElse(false);

        String reviewReason = """
            Не найдено прямое подтверждение указанного опыта
            в резюме или дополнительных фактах кандидата.
            """.replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();

        List<String> evidence = new ArrayList<>();

        evidence.add(
                "REVIEW:Прямое подтверждение не найдено "
                        + "в резюме или дополнительных фактах."
        );

        if (draftsAllowed
                && isProfessionalCapabilityQuestion(
                question.questionText()
        )) {
            evidence.add(
                    "POLICY:related_experience_draft_enabled"
            );

            return new GeneratedHhQuestionnaireAnswerDto(
                    question.fieldName(),
                    buildReviewDraft(question.questionText()),
                    HhQuestionnaireAnswerQuality.REVIEW_REQUIRED,
                    reviewReason,
                    List.copyOf(evidence)
            );
        }

        return new GeneratedHhQuestionnaireAnswerDto(
                question.fieldName(),
                "",
                HhQuestionnaireAnswerQuality.REVIEW_REQUIRED,
                reviewReason,
                List.copyOf(evidence)
        );
    }

    private GeneratedHhQuestionnaireAnswerDto
    buildStructuredProfileReviewRequiredAnswer(
            HhQuestionnaireQuestionDto question,
            Optional<CandidateQuestionnaireProfileDto> optionalProfile
    ) {
        String normalizedQuestion = normalizeForSearch(
                question.questionText()
        );

        String reviewReason;

        if (optionalProfile.isEmpty()) {
            reviewReason =
                    "Профиль кандидата не сохранён: "
                            + "автоматический ответ невозможен.";
        } else if (isTestAssignmentQuestion(normalizedQuestion)) {
            reviewReason =
                    "В профиле кандидата не указана готовность "
                            + "выполнить тестовое задание.";
        } else if (isSalaryQuestion(normalizedQuestion)) {
            reviewReason =
                    "Основа суммы зарплаты в профиле не совпадает "
                            + "с формулировкой вопроса работодателя "
                            + "или не указана.";
        } else {
            reviewReason =
                    "Для автоматического ответа не хватает "
                            + "подтверждённых фактов профиля кандидата.";
        }

        return new GeneratedHhQuestionnaireAnswerDto(
                question.fieldName(),
                "",
                HhQuestionnaireAnswerQuality.REVIEW_REQUIRED,
                reviewReason,
                List.of("REVIEW:PROFILE_FACT_MISSING")
        );
    }

    private String buildReviewDraft(String questionText) {
        ReviewQuestionSubject subject =
                extractReviewQuestionSubject(questionText);

        if (subject.subject().isBlank()) {
            return """
                Прямое подтверждение по этому вопросу в резюме
                не найдено. Готов обсудить релевантный опыт
                на интервью.
                """.replace("\n", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
        }

        return switch (subject.kind()) {
            case SKILL_LEVEL -> """
                Прямое подтверждение уровня владения %s в резюме
                не найдено. Готов обсудить релевантный опыт
                и задачи на интервью.
                """.formatted(subject.subject())
                    .replace("\n", " ")
                    .replaceAll("\\s+", " ")
                    .trim();

            case EXPERIENCE -> """
                Прямой опыт работы с %s в резюме не указан.
                Готов обсудить смежный опыт и применимость
                навыков к задачам проекта.
                """.formatted(subject.subject())
                    .replace("\n", " ")
                    .replaceAll("\\s+", " ")
                    .trim();

            case GENERIC -> """
                Прямое подтверждение по этому вопросу в резюме
                не найдено. Готов обсудить релевантный опыт
                на интервью.
                """.replace("\n", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
        };
    }

    private ReviewQuestionSubject extractReviewQuestionSubject(
            String questionText
    ) {
        String question = compact(questionText);

        Matcher skillLevelMatcher =
                SKILL_LEVEL_QUESTION_PATTERN.matcher(question);

        if (skillLevelMatcher.matches()) {
            return new ReviewQuestionSubject(
                    compact(skillLevelMatcher.group(1)),
                    ReviewQuestionKind.SKILL_LEVEL
            );
        }

        Matcher experienceMatcher =
                EXPERIENCE_QUESTION_PATTERN.matcher(question);

        if (experienceMatcher.matches()) {
            return new ReviewQuestionSubject(
                    compact(experienceMatcher.group(1)),
                    ReviewQuestionKind.EXPERIENCE
            );
        }

        return new ReviewQuestionSubject(
                "",
                ReviewQuestionKind.GENERIC
        );
    }


    private boolean isProfessionalCapabilityQuestion(String questionText) {
        String question = normalizeForSearch(questionText);

        return containsAnyFragment(
                question,
                "опыт",
                "навык",
                "владен",
                "знан",
                "технолог",
                "инструмент",
                "библиотек",
                "фреймворк",
                "тестирован",
                "разработ",
                "архитектур",
                "программир",
                "автоматизац",
                "администр",
                "аналитик",
                "дизайн",
                "верстк",
                "интеграц",
                "проектир",
                "база дан",
                "api",
                "devops",
                "ci cd"
        );
    }

    private boolean containsNormalizedText(
            String sourceText,
            String quote
    ) {
        String normalizedSource = normalizeForContainment(sourceText);
        String normalizedQuote = normalizeForContainment(quote);

        return !normalizedQuote.isBlank()
                && normalizedSource.contains(normalizedQuote);
    }

    private Set<String> meaningfulTokens(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }

        Set<String> result = new LinkedHashSet<>();
        Matcher matcher = TOKEN_PATTERN.matcher(
                Normalizer.normalize(value, Normalizer.Form.NFKC)
        );

        while (matcher.find()) {
            String token = normalizeToken(matcher.group());

            if (isMeaningfulToken(token)) {
                result.add(token);
            }
        }

        return Set.copyOf(result);
    }

    private Set<String> protectedTokens(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }

        Set<String> result = new LinkedHashSet<>();
        Matcher matcher = PROTECTED_TOKEN_PATTERN.matcher(
                Normalizer.normalize(value, Normalizer.Form.NFKC)
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

    private boolean intersects(
            Collection<String> left,
            Collection<String> right
    ) {
        for (String value : left) {
            if (right.contains(value)) {
                return true;
            }
        }

        return false;
    }

    private boolean containsAnyFragment(
            String value,
            String... fragments
    ) {
        for (String fragment : fragments) {
            if (value.contains(fragment)) {
                return true;
            }
        }

        return false;
    }

    private String normalizeForSearch(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{Nd}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeForContainment(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeToken(String token) {
        return token == null
                ? ""
                : Normalizer.normalize(token, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private String compact(String value) {
        return value == null
                ? ""
                : value.replaceAll("\\s+", " ").trim();
    }

    private String limit(String value) {
        String compactValue = compact(value);

        if (compactValue.length() <= MAX_EVIDENCE_LENGTH) {
            return compactValue;
        }

        return compactValue.substring(
                0,
                MAX_EVIDENCE_LENGTH - 1
        ).strip() + "…";
    }

    private String formatAmount(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private String formatStartAvailability(
            CandidateQuestionnaireProfileDto profile
    ) {
        return switch (profile.startAvailability()) {
            case IMMEDIATELY -> "Готов начать работу сразу.";
            case WITHIN_TWO_WEEKS ->
                    "Готов начать работу в течение двух недель.";
            case WITHIN_ONE_MONTH ->
                    "Готов начать работу в течение месяца.";
            case NEGOTIABLE ->
                    "Срок начала работы готов обсудить.";
        };
    }

    private String formatWorkPreference(
            CandidateQuestionnaireProfileDto profile
    ) {
        String answer = switch (profile.workFormatPreference()) {
            case ANY -> "Рассматриваю любой формат работы.";
            case REMOTE -> "Предпочитаю удалённый формат работы.";
            case HYBRID -> "Предпочитаю гибридный формат работы.";
            case OFFICE -> "Рассматриваю офисный формат работы.";
        };

        if (profile.remoteWorkPriority()
                && profile.workFormatPreference().name().equals("ANY")) {
            return answer + " Удалённый формат в приоритете.";
        }

        return answer;
    }

    private enum ReviewQuestionKind {
        SKILL_LEVEL,
        EXPERIENCE,
        GENERIC
    }

    private record ReviewQuestionSubject(
            String subject,
            ReviewQuestionKind kind
    ) {
    }

    private record CandidateEvidenceSources(
            String resumeText,
            String additionalFacts
    ) {

        private static CandidateEvidenceSources from(
                GenerateHhQuestionnaireAnswersCommand command,
                Optional<CandidateQuestionnaireProfileDto> optionalProfile
        ) {
            return new CandidateEvidenceSources(
                    command.resumeText(),
                    optionalProfile
                            .map(
                                    CandidateQuestionnaireProfileDto
                                            ::additionalConfirmedFacts
                            )
                            .orElse("")
            );
        }
    }

    private record GroundedEvidence(
            HhQuestionnaireEvidenceSource source,
            String quote
    ) {
    }

    private record ProfileFactResolution(
            String answer,
            String evidence
    ) {
    }
}

record HhQuestionnaireAnswerProposal(
        String fieldName,
        String topic,
        String answer,
        String evidenceSource,
        String evidenceQuote
) {

    HhQuestionnaireAnswerProposal {
        fieldName = requireNotBlank(
                fieldName,
                "Questionnaire proposal field name must not be blank"
        );
        topic = normalize(topic);
        answer = normalize(answer);
        evidenceSource = normalize(evidenceSource);
        evidenceQuote = normalize(evidenceQuote);
    }

    private static String requireNotBlank(
            String value,
            String message
    ) {
        String normalized = normalize(value);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}

enum HhQuestionnaireEvidenceSource {

    RESUME,
    PROFILE_ADDITIONAL,
    NONE;

    static HhQuestionnaireEvidenceSource from(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }

        try {
            return HhQuestionnaireEvidenceSource.valueOf(
                    value.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Unsupported questionnaire evidence source: " + value,
                    exception
            );
        }
    }
}
