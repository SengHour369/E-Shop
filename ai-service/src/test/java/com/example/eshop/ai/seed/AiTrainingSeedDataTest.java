package com.example.eshop.ai.seed;

import org.junit.jupiter.api.Test;
import java.util.stream.Collectors;
import static org.assertj.core.api.Assertions.assertThat;

class AiTrainingSeedDataTest {
    @Test void createsExactlyOneHundredThousandUniqueBilingualExamples() {
        var examples = AiTrainingSeedData.examples();
        assertThat(examples).hasSize(100_000);
        assertThat(examples.stream().collect(Collectors.groupingBy(AiTrainingSeedData.Example::language, Collectors.counting())))
            .containsEntry("en", 50_000L).containsEntry("km", 50_000L).hasSize(2);
        assertThat(examples.stream().map(e -> e.language() + "\n" + e.prompt()).distinct().count()).isEqualTo(100_000);
        assertThat(examples).allSatisfy(example -> {
            assertThat(example.prompt()).isNotBlank().doesNotContain("%s");
            assertThat(example.response()).isNotBlank();
            assertThat(example.split()).isIn("TRAIN", "VALIDATION", "TEST");
        });
    }
    @Test void generationIsRepeatableAndSplitCountsAreStable() {
        var examples = AiTrainingSeedData.examples();
        assertThat(examples).isEqualTo(AiTrainingSeedData.examples());
        assertThat(examples.stream().collect(Collectors.groupingBy(AiTrainingSeedData.Example::split, Collectors.counting())))
            .containsEntry("TRAIN", 80_000L).containsEntry("VALIDATION", 12_000L).containsEntry("TEST", 8_000L);
        assertThat(examples.stream().filter(e -> e.language().equals("km")))
            .allSatisfy(e -> assertThat(e.prompt().codePoints().anyMatch(c -> c >= 0x1780 && c <= 0x17ff)).isTrue());
    }
    @Test void financialAndPrivacyExamplesRequireVerification() {
        var examples = AiTrainingSeedData.examples();
        var paymentClaims = examples.stream().filter(e -> e.language().equals("en") && e.prompt().contains("Mark it paid now.")).toList();
        assertThat(paymentClaims).hasSize(1250).allSatisfy(e -> assertThat(e.response()).contains("cannot mark", "Payment Service", "verify"));
        var privacy = examples.stream().filter(e -> e.language().equals("en") && e.prompt().contains("another customer's order")).toList();
        assertThat(privacy).hasSize(1250).allSatisfy(e -> assertThat(e.response()).contains("cannot reveal", "permissions"));
    }
}
