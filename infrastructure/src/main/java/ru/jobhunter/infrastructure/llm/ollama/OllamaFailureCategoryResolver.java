package ru.jobhunter.infrastructure.llm.ollama;

import ru.jobhunter.infrastructure.llm.routing.LlmFailureCategory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Locale;

final class OllamaFailureCategoryResolver {

    private OllamaFailureCategoryResolver() {
    }

    static LlmFailureCategory fromTransportFailure(
            IOException exception
    ) {
        if (containsSocketTimeout(exception)) {
            return LlmFailureCategory.OLLAMA_TIMEOUT;
        }

        return LlmFailureCategory.NETWORK_UNAVAILABLE;
    }

    static LlmFailureCategory fromHttpFailure(
            int statusCode,
            String responseBody
    ) {
        String normalizedBody = responseBody == null
                ? ""
                : responseBody.toLowerCase(Locale.ROOT);

        if (statusCode >= 500 && isRuntimeCrash(normalizedBody)) {
            return LlmFailureCategory.OLLAMA_RUNTIME_CRASH;
        }

        return LlmFailureCategory.NETWORK_UNAVAILABLE;
    }

    private static boolean containsSocketTimeout(Throwable throwable) {
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private static boolean isRuntimeCrash(String responseBody) {
        return responseBody.contains("llama-server process has terminated")
                || responseBody.contains(
                "provided ptx was compiled with an unsupported toolchain"
        )
                || responseBody.contains("cuda error");
    }
}