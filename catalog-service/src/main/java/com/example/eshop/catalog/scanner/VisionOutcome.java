package com.example.eshop.catalog.scanner;

import java.util.List;

public record VisionOutcome(boolean available, List<VisionLabel> labels) {
    public static VisionOutcome unavailable() {
        return new VisionOutcome(false, List.of());
    }
}
