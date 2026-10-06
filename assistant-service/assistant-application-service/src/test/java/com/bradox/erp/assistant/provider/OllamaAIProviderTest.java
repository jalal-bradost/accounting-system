package com.bradox.erp.assistant.provider;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OllamaAIProviderTest {

    @Test
    void stripThinkingRemovesWrappedBlock() {
        String open = "<" + "think>";
        String close = "</" + "think>";
        String input = open + "\nI should call a tool.\n" + close + "\n\nNet profit was 17,710 IQD.";
        assertThat(OllamaAIProvider.stripThinking(input)).isEqualTo("Net profit was 17,710 IQD.");
    }

    @Test
    void stripThinkingKeepsTextAfterOrphanClose() {
        String close = "</" + "think>";
        String input = "Okay, let me break this down...\n" + close + "\n\nOur net profit was 17,710 IQD.";
        assertThat(OllamaAIProvider.stripThinking(input)).isEqualTo("Our net profit was 17,710 IQD.");
    }

    @Test
    void stripThinkingLeavesCleanAnswersAlone() {
        assertThat(OllamaAIProvider.stripThinking("Our net profit was 17,710 IQD."))
                .isEqualTo("Our net profit was 17,710 IQD.");
    }
}
