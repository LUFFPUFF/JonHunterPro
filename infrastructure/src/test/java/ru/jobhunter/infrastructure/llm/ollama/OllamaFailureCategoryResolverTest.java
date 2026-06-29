package ru.jobhunter.infrastructure.llm.ollama;

import org.junit.jupiter.api.Test;
import ru.jobhunter.infrastructure.llm.routing.LlmFailureCategory;

import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OllamaFailureCategoryResolverTest {

    @Test
    void shouldClassifyCudaPtxServerFailureAsRuntimeCrash() {
        LlmFailureCategory category =
                OllamaFailureCategoryResolver.fromHttpFailure(
                        500,
                        "llama-server process has terminated: "
                                + "CUDA error: the provided PTX was compiled "
                                + "with an unsupported toolchain"
                );

        assertEquals(
                LlmFailureCategory.OLLAMA_RUNTIME_CRASH,
                category
        );
    }

    @Test
    void shouldClassifySocketTimeoutAsOllamaTimeout() {
        LlmFailureCategory category =
                OllamaFailureCategoryResolver.fromTransportFailure(
                        new SocketTimeoutException("Read timed out")
                );

        assertEquals(
                LlmFailureCategory.OLLAMA_TIMEOUT,
                category
        );
    }
}