package com.example.eshop.ai.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import static org.assertj.core.api.Assertions.*;

class AiTrainingScaleTest {
    @Test void contextsAreUniqueAndPreserveProductSplitsAndResponses() {
        var keys = new HashSet<String>();
        for (int context = 0; context < 100; context++) {
            for (int base : new int[]{0,1,2498,2499,99998,99999}) {
                var row = AiTrainingSeedData.exampleAt(context * 100_000 + base);
                var original = AiTrainingSeedData.exampleAt(base);
                assertThat(keys.add(row.language() + row.prompt())).isTrue();
                assertThat(row.response()).isEqualTo(original.response());
                assertThat(row.split()).isEqualTo(original.split());
                assertThat(row.language()).isEqualTo(base % 2 == 0 ? "en" : "km");
            }
        }
        assertThat(AiTrainingSeedData.exampleAt(0).prompt()).isEqualTo("Please help me: Find wireless headphones.");
    }
    @Test void batchesCanCrossContextBoundaryAndResumeExactly() {
        var batch = AiTrainingSeedData.batch(99_998, 6);
        assertThat(batch).hasSize(6);
        for (int i=0; i<6; i++) assertThat(batch.get(i)).isEqualTo(AiTrainingSeedData.exampleAt(99_998+i));
        assertThat(AiTrainingSeedData.batch(9_999_998,2)).hasSize(2);
        assertThatThrownBy(() -> AiTrainingSeedData.batch(0,10_001)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AiTrainingSeedData.exampleAt(10_000_000)).isInstanceOf(IndexOutOfBoundsException.class);
    }
    @Test void exportedJsonIsBilingualDraftAndEscapesQuotesAndNewlines() throws Exception {
        var mapper = new ObjectMapper();
        for (int index : new int[]{0,1,100_000,100_001,9_999_999}) {
            var row = AiTrainingSeedData.exampleAt(index);
            var json = mapper.readTree(AiTrainingDataExporter.toJson(row));
            assertThat(json.get("review_status").asText()).isEqualTo("DRAFT");
            assertThat(json.get("source_verified").asBoolean()).isFalse();
            assertThat(json.get("messages").get(0).get("content").asText()).isEqualTo(row.prompt());
            assertThat(json.get("messages").get(1).get("content").asText()).isEqualTo(row.response());
        }
        var escaped = new AiTrainingSeedData.Example("en", "a\"b\\c\nd\t\u0001", "ok", "TRAIN");
        assertThat(mapper.readTree(AiTrainingDataExporter.toJson(escaped)).get("messages").get(0).get("content").asText()).isEqualTo(escaped.prompt());
    }
}
