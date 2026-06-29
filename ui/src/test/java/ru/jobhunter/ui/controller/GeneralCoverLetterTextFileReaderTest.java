package ru.jobhunter.ui.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeneralCoverLetterTextFileReaderTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void shouldReadUtf8TextAndRemoveBom() throws Exception {
        Path file = temporaryDirectory.resolve("cover-letter.txt");
        Files.writeString(file, "\uFEFFЗдравствуйте!\n\nБуду рад обсудить вакансию.", StandardCharsets.UTF_8);
        String content = GeneralCoverLetterTextFileReader.read(file);
        assertEquals("Здравствуйте!\n\nБуду рад обсудить вакансию.", content);
    }

    @Test
    void shouldRejectBlankTextFile() throws Exception {
        Path file = temporaryDirectory.resolve("blank.txt");
        Files.writeString(file, " \n ", StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> GeneralCoverLetterTextFileReader.read(file));
    }

    @Test
    void shouldRejectOversizedTextFile() throws Exception {
        Path file = temporaryDirectory.resolve("large.txt");
        Files.writeString(file, "x".repeat(64 * 1024 + 1), StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> GeneralCoverLetterTextFileReader.read(file));
    }
}