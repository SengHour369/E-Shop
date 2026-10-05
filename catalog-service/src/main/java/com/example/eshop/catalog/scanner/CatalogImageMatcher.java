package com.example.eshop.catalog.scanner;

import com.example.eshop.catalog.dto.response.ImageCandidate;
import com.example.eshop.catalog.dto.response.ImageMatchResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Keeps only products the catalog search returns. Model names never become product ids. */
public final class CatalogImageMatcher {
    public static final double EXACT_CONFIDENCE = 0.90;

    public record ProductMatch(Long productId, Long productSkuId, String name, String sku, String imageUrl) {}

    @FunctionalInterface
    public interface NameSearch {
        List<ProductMatch> find(String name);
    }

    private CatalogImageMatcher() {}

    public static ImageMatchResponse match(List<VisionLabel> labels, NameSearch search, String requestId) {
        if (labels == null || labels.isEmpty()) {
            return none(requestId, null);
        }
        Map<Long, ImageCandidate> byProduct = new LinkedHashMap<>();
        double topModelConfidence = 0;
        for (VisionLabel label : labels) {
            if (label == null || label.name() == null || label.name().isBlank() || Double.isNaN(label.confidence())) {
                continue;
            }
            double confidence = clamp(label.confidence());
            topModelConfidence = Math.max(topModelConfidence, confidence);
            for (ProductMatch product : search.find(label.name())) {
                if (product == null || product.productId() == null || product.name() == null) {
                    continue;
                }
                ImageCandidate candidate = new ImageCandidate(product.productId(), product.productSkuId(),
                        product.name(), product.sku(), product.imageUrl(), confidence);
                byProduct.merge(product.productId(), candidate,
                        (left, right) -> left.getConfidence() >= right.getConfidence() ? left : right);
            }
        }
        List<ImageCandidate> candidates = byProduct.values().stream()
                .sorted(Comparator.comparingDouble(ImageCandidate::getConfidence).reversed()
                        .thenComparing(ImageCandidate::getProductId))
                .limit(5)
                .toList();
        if (candidates.isEmpty()) {
            return none(requestId, topModelConfidence);
        }
        ImageCandidate best = candidates.get(0);
        boolean exact = candidates.size() == 1
                && best.getConfidence() >= EXACT_CONFIDENCE
                && labels.stream().anyMatch(label -> label != null
                && clamp(label.confidence()) >= EXACT_CONFIDENCE
                && sameName(label.name(), best.getName()));
        return ImageMatchResponse.builder()
                .matchType("AI_IMAGE")
                .confidence(round(best.getConfidence()))
                .exact(exact)
                .requiresSelection(!exact)
                .message(exact ? "Product recognized" : "Select the matching product")
                .requestId(requestId)
                .candidates(new ArrayList<>(candidates))
                .build();
    }

    public static ImageMatchResponse unavailable(String requestId) {
        return ImageMatchResponse.builder()
                .matchType("AI_UNAVAILABLE")
                .exact(false)
                .requiresSelection(false)
                .message("Product recognition is unavailable")
                .requestId(requestId)
                .candidates(List.of())
                .build();
    }

    private static ImageMatchResponse none(String requestId, Double confidence) {
        return ImageMatchResponse.builder()
                .matchType("NO_CATALOG_MATCH")
                .confidence(confidence == null ? null : round(confidence))
                .exact(false)
                .requiresSelection(false)
                .message("No matching catalog product")
                .requestId(requestId)
                .candidates(List.of())
                .build();
    }

    private static boolean sameName(String left, String right) {
        return left != null && right != null && collapse(left).equals(collapse(right));
    }

    private static String collapse(String value) {
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static double clamp(double confidence) {
        if (confidence < 0) return 0;
        return Math.min(confidence, 1);
    }

    private static double round(double confidence) {
        return Math.round(confidence * 100.0) / 100.0;
    }
}
