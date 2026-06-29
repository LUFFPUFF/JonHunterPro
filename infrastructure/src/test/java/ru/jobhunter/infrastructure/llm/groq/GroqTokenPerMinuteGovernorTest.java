package ru.jobhunter.infrastructure.llm.groq;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroqTokenPerMinuteGovernorTest {

    @Test
    void shouldDelayRequestWhenPairExceedsTpmLimit() {
        AtomicLong clock = new AtomicLong(0L);

        GroqTokenPerMinuteGovernor governor =
                new GroqTokenPerMinuteGovernor(
                        8_000,
                        clock::get
                );

        assertEquals(
                Duration.ZERO,
                governor.reserveTokenBudget(4_187)
        );

        assertEquals(
                Duration.ofMillis(2_970),
                governor.reserveTokenBudget(4_209)
        );
    }

    @Test
    void shouldNotDelayRequestsInsideTpmLimit() {
        AtomicLong clock = new AtomicLong(0L);

        GroqTokenPerMinuteGovernor governor =
                new GroqTokenPerMinuteGovernor(
                        8_000,
                        clock::get
                );

        assertEquals(
                Duration.ZERO,
                governor.reserveTokenBudget(4_000)
        );

        assertEquals(
                Duration.ZERO,
                governor.reserveTokenBudget(4_000)
        );
    }

    @Test
    void shouldRejectSingleRequestLargerThanBudget() {
        AtomicLong clock = new AtomicLong(0L);

        GroqTokenPerMinuteGovernor governor =
                new GroqTokenPerMinuteGovernor(
                        7_200,
                        clock::get
                );

        IllegalArgumentException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () -> governor.reserveTokenBudget(7_201)
                );

        assertTrue(
                exception.getMessage()
                        .contains("Requested Groq token budget")
        );
    }
}