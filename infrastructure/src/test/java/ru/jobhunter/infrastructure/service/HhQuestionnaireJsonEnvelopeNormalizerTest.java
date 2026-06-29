package ru.jobhunter.infrastructure.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HhQuestionnaireJsonEnvelopeNormalizerTest {

    @Test
    void shouldWrapRootArrayIntoAnswersObject() {
        String result = HhQuestionnaireJsonEnvelopeNormalizer.normalize(
                """
                [
                  {
                    "questionIndex": 1,
                    "status": "CANDIDATE_FACT_REQUIRED",
                    "answer": "",
                    "selectedOptionIndex": 0,
                    "missingFact": "net salary after tax"
                  }
                ]
                """
        );

        assertEquals(
                """
                {"answers":[
                  {
                    "questionIndex": 1,
                    "status": "CANDIDATE_FACT_REQUIRED",
                    "answer": "",
                    "selectedOptionIndex": 0,
                    "missingFact": "net salary after tax"
                  }
                ]}""",
                result
        );
    }

    @Test
    void shouldKeepRootObjectUnchanged() {
        String json = """
                {
                  "answers": [
                    {
                      "questionIndex": 1,
                      "status": "ANSWER",
                      "answer": "Да",
                      "selectedOptionIndex": 0,
                      "missingFact": ""
                    }
                  ]
                }
                """;

        assertEquals(
                json.strip(),
                HhQuestionnaireJsonEnvelopeNormalizer.normalize(json)
        );
    }

    @Test
    void shouldRemoveJsonCodeFence() {
        String result = HhQuestionnaireJsonEnvelopeNormalizer.normalize(
                """
                ```json
                [{"questionIndex":1,"status":"ANSWER","answer":"Да","selectedOptionIndex":0,"missingFact":""}]
                ```
                """
        );

        assertEquals(
                """
                {"answers":[{"questionIndex":1,"status":"ANSWER","answer":"Да","selectedOptionIndex":0,"missingFact":""}]}""",
                result
        );
    }

    @Test
    void shouldRejectResponseWithoutJson() {
        assertThrows(
                IllegalStateException.class,
                () -> HhQuestionnaireJsonEnvelopeNormalizer.normalize(
                        "Ответ отсутствует"
                )
        );
    }
}