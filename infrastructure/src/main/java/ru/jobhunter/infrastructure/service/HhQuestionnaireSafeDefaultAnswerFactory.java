package ru.jobhunter.infrastructure.service;

import ru.jobhunter.core.application.dto.GeneratedHhQuestionnaireAnswerDto;
import ru.jobhunter.core.application.dto.HhQuestionnaireAnswerQuality;
import ru.jobhunter.core.application.dto.HhQuestionnaireQuestionDto;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class HhQuestionnaireSafeDefaultAnswerFactory {

    private HhQuestionnaireSafeDefaultAnswerFactory() {
    }

    static Optional<GeneratedHhQuestionnaireAnswerDto> tryCreate(
            int questionIndex,
            HhQuestionnaireQuestionDto question
    ) {
        if (question.isText()) {
            return Optional.of(create(
                    questionIndex,
                    question.fieldName(),
                    fallbackText(question),
                    ""
            ));
        }

        if (question.isRadioWithOtherText()) {
            return Optional.of(create(
                    questionIndex,
                    question.fieldName(),
                    fallbackText(question),
                    question.otherOptionValue()
            ));
        }

        return Optional.empty();
    }

    private static GeneratedHhQuestionnaireAnswerDto create(
            int questionIndex,
            String fieldName,
            String answer,
            String selectedOptionValue
    ) {
        return new GeneratedHhQuestionnaireAnswerDto(
                fieldName,
                answer,
                selectedOptionValue,
                HhQuestionnaireAnswerQuality.SAFE_DEFAULT,
                "",
                List.of(
                        "LLM_FORM:questionIndex=" + questionIndex,
                        "SAFE_DEFAULT:CANDIDATE_FACT_NOT_CONFIRMED"
                )
        );
    }

    private static String fallbackText(
            HhQuestionnaireQuestionDto question
    ) {
        String text = question.questionText()
                .toLowerCase(Locale.ROOT);

        if (containsAny(
                text,
                "зарплат",
                "оклад",
                "доход",
                "финансов",
                "на руки",
                "ндфл",
                "вознагражден"
        )) {
            return """
                Ожидания обсуждаемы и зависят от задач, уровня
                ответственности, формата работы и полного пакета условий.
                """.replaceAll("\\s+", " ").strip();
        }

        if (containsAny(
                text,
                "что для вас важно",
                "важно в работе",
                "критери",
                "приоритет",
                "предпочтени"
        )) {
            return """
                Важны интересные задачи, профессиональное развитие,
                понятные цели, конструктивная команда и качественные
                рабочие процессы.
                """.replaceAll("\\s+", " ").strip();
        }

        if (containsAny(
                text,
                "docker",
                "kubernetes",
                "openshift",
                "контейнер"
        )) {
            return """
                Готов освоить используемые в команде инструменты
                и применять их в рабочих задачах.
                """.replaceAll("\\s+", " ").strip();
        }

        if (containsAny(
                text,
                "devops",
                "ci/cd",
                "pipeline",
                "пайплайн"
        )) {
            return """
                Готов изучить используемые в команде процессы
                автоматизации и рабочие инструменты.
                """.replaceAll("\\s+", " ").strip();
        }

        if (containsAny(
                text,
                "военн",
                "приписн"
        )) {
            return """
                Необходимую информацию и документы готов предоставить
                при дальнейшем оформлении.
                """.replaceAll("\\s+", " ").strip();
        }

        if (containsAny(
                text,
                "самозанят",
                "статус ип",
                "индивидуальн",
                "ооо"
        )) {
            return """
                Формат сотрудничества готов обсудить
                при дальнейшем взаимодействии.
                """.replaceAll("\\s+", " ").strip();
        }

        if (containsAny(
                text,
                "образован",
                "университет",
                "вуз"
        )) {
            return """
                Информацию об образовании и подтверждающие документы
                готов предоставить при дальнейшем общении.
                """.replaceAll("\\s+", " ").strip();
        }

        if (containsAny(
                text,
                "причин",
                "поиск работы",
                "смена работы"
        )) {
            return """
                Рассматриваю новую позицию для профессионального развития,
                участия в интересных задачах и расширения практического опыта.
                """.replaceAll("\\s+", " ").strip();
        }

        return """
            Готов обсудить детали по данному вопросу
            при дальнейшем взаимодействии.
            """.replaceAll("\\s+", " ").strip();
    }

    private static boolean containsAny(
            String value,
            String... markers
    ) {
        for (String marker : markers) {
            if (value.contains(marker)) {
                return true;
            }
        }

        return false;
    }
}