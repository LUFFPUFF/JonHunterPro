package ru.jobhunter.ui.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

final class GeneralCoverLetterTextFileReader {
    private static final long MAX_FILE_SIZE_BYTES = 64 * 1024L;

    private GeneralCoverLetterTextFileReader() {
    }

    static String read(Path file) throws IOException {
        Objects.requireNonNull(file, "General cover letter file must not be null");
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Выбранный путь не является файлом");
        }
        long fileSize = Files.size(file);
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("TXT-файл сопроводительного письма не должен превышать 64 КБ");
        }
        String content = Files.readString(file, StandardCharsets.UTF_8);
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }
        if (content.isBlank()) {
            throw new IllegalArgumentException("TXT-файл сопроводительного письма пуст");
        }
        return content;
    }
}
