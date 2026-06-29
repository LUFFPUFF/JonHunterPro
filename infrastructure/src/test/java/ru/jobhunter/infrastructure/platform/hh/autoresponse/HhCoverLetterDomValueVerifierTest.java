package ru.jobhunter.infrastructure.platform.hh.autoresponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HhCoverLetterDomValueVerifierTest {

    @Test
    void shouldVerifyIdenticalCoverLetterText() {
        HhCoverLetterDomValueVerification verification =
                HhCoverLetterDomValueVerifier.verify(
                        "Здравствуйте!\nБуду рад обсудить вакансию.",
                        "Здравствуйте!\nБуду рад обсудить вакансию."
                );

        assertTrue(verification.matches());
        assertEquals(
                verification.expectedLength(),
                verification.actualLength()
        );
    }

    @Test
    void shouldTreatDifferentLineEndingsAsEqual() {
        HhCoverLetterDomValueVerification verification =
                HhCoverLetterDomValueVerifier.verify(
                        "Здравствуйте!\n\nБуду рад обсудить вакансию.",
                        "Здравствуйте!\r\n\r\nБуду рад обсудить вакансию."
                );

        assertTrue(verification.matches());
    }

    @Test
    void shouldRejectIncompleteTextareaValue() {
        HhCoverLetterDomValueVerification verification =
                HhCoverLetterDomValueVerifier.verify(
                        "Здравствуйте!\nБуду рад обсудить вакансию.",
                        "Здравствуйте!"
                );

        assertFalse(verification.matches());
        assertTrue(
                verification.expectedLength()
                        > verification.actualLength()
        );
    }

    @Test
    void shouldRejectBlankExpectedCoverLetter() {
        assertThrows(
                IllegalArgumentException.class,
                () -> HhCoverLetterDomValueVerifier.verify(
                        "   ",
                        "Здравствуйте!"
                )
        );
    }
}