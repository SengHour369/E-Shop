package com.example.eshop.ai.dto;

import java.util.List;

/** Server-selected views; never consume model-specified component names or URLs. */
public record ChatPresentation(String schemaVersion, List<Block> blocks) {
    public record Block(String kind) { }
    public static ChatPresentation inventory(boolean lowStock) {
        return new ChatPresentation("1.0", List.of(new Block(lowStock ? "inventory.low-stock" : "inventory.list")));
    }
    public static ChatPresentation orders() {
        return new ChatPresentation("1.0", List.of(new Block("orders.list")));
    }
}
