package ru.jobhunter.infrastructure.platform.hh.autoresponse;

record HhCoverLetterDomValueVerification(
        boolean matches,
        int expectedLength,
        int actualLength
) {
}