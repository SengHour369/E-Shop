package com.example.eshop.catalog.scanner;

import com.example.eshop.catalog.dto.response.*;
import com.example.eshop.common.request.RequestIds;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/** Compares actual catalog images with the uploaded photo. URLs never come from model output. */
@Component
public class CatalogVisualComparison {

    private final ObjectMapper mapper;
    private final URI endpoint;
    private final String key, cloud;
    private final HttpClient http;

    @org.springframework.beans.factory.annotation.Autowired
    public CatalogVisualComparison(
            ObjectMapper mapper,
            @Value("${scanner.ai-inference-url:http://localhost:8000}") String inference,
            @Value("${scanner.vision-service-key:${AI_VISION_SERVICE_KEY:}}") String key,
            @Value("${cloudinary.cloud_name:}") String cloud
    ) {
        this(
                mapper,
                inference,
                key,
                cloud,
                HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .connectTimeout(Duration.ofSeconds(3))
                        .build()
        );
    }

    CatalogVisualComparison(
            ObjectMapper mapper,
            String inference,
            String key,
            String cloud,
            HttpClient http
    ) {
        this.http = http;
        this.mapper = mapper;
        this.endpoint = URI.create(
                inference.replaceAll("/$", "")
                        + "/api/v1/ai/vision/compare"
        );
        this.key = key;
        this.cloud = cloud;
    }

    public ImageMatchResponse compare(
            MultipartFile photo,
            ImageMatchResponse labels
    ) {
        if (photo.getSize() > 8 * 1024 * 1024) {
            return CatalogImageMatcher.unavailable(labels.getRequestId());
        }

        try {
            return compare(
                    photo.getBytes(),
                    photo.getContentType(),
                    labels
            );
        } catch (java.io.IOException invalid) {
            return CatalogImageMatcher.unavailable(labels.getRequestId());
        }
    }

    public ImageMatchResponse compare(
            byte[] original,
            String mediaType,
            ImageMatchResponse labels
    ) {
        if (labels.getCandidates().isEmpty()) {
            return labels;
        }

        ImageMatchResponse fallback = ImageMatchResponse.builder()
                .matchType("AI_LABEL_CANDIDATES")
                .exact(false)
                .requiresSelection(true)
                .message(
                        "Select a catalog candidate; visual similarity "
                                + "has not been verified"
                )
                .requestId(labels.getRequestId())
                .candidates(labels.getCandidates())
                .build();

        if (key.isBlank()
                || cloud.isBlank()
                || original.length > 8 * 1024 * 1024) {
            return fallback;
        }

        try {
            var candidates = new ArrayList<ImageCandidate>();
            var images = new ArrayList<Map<String, String>>();

            for (var candidate : labels.getCandidates()) {
                Map<String, String> image = download(candidate.getImageUrl());

                if (image != null) {
                    candidates.add(candidate);
                    images.add(image);
                }
            }

            if (images.isEmpty()) {
                return fallback;
            }

            var body = Map.of(
                    "mediaType", mediaType,
                    "imageBase64", Base64.getEncoder()
                            .encodeToString(original),
                    "candidates", images
            );
            var builder = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("X-Vision-Service-Key", key)
                    .POST(
                            HttpRequest.BodyPublishers.ofString(
                                    mapper.writeValueAsString(body)
                            )
                    );

            if (RequestIds.current() != null) {
                builder.header(RequestIds.HEADER, RequestIds.current());
            }

            var response = http.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofInputStream()
            );
            byte[] bytes;

            try (var input = response.body()) {
                bytes = input.readNBytes(65537);
            }

            if (response.statusCode() != 200 || bytes.length > 65536) {
                return fallback;
            }

            var root = mapper.readTree(bytes);

            if (!root.path("matches").isArray()) {
                return fallback;
            }

            var scores = new HashMap<Integer, Double>();

            for (var row : root.path("matches")) {
                int index = row.path("index").asInt(-1);
                double score = row.path("confidence").asDouble(Double.NaN);

                if (row.path("index").isIntegralNumber()
                        && row.path("index").canConvertToInt()
                        && row.path("confidence").isNumber()
                        && index >= 0
                        && index < candidates.size()
                        && Double.isFinite(score)
                        && score >= 0
                        && score <= 1) {
                    scores.merge(index, score, Math::max);
                }
            }

            var ranked = scores.entrySet().stream()
                    .sorted(
                            Map.Entry.<Integer, Double>comparingByValue()
                                    .reversed()
                    )
                    .map(entry -> {
                        var c = candidates.get(entry.getKey());
                        return new ImageCandidate(
                                c.getProductId(),
                                c.getProductSkuId(),
                                c.getName(),
                                c.getSku(),
                                c.getImageUrl(),
                                entry.getValue()
                        );
                    })
                    .toList();

            // A comparison confidence never authorizes a purchase or bypasses customer selection.
            return ImageMatchResponse.builder()
                    .matchType("AI_VISUAL_COMPARISON")
                    .exact(false)
                    .requiresSelection(!ranked.isEmpty())
                    .confidence(
                            ranked.isEmpty()
                                    ? null
                                    : ranked.get(0).getConfidence()
                    )
                    .requestId(labels.getRequestId())
                    .message(
                            ranked.isEmpty()
                                    ? "No visually matching catalog product"
                                    : "Select the visually matching catalog product"
                    )
                    .candidates(ranked)
                    .build();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return fallback;
        } catch (Exception unavailable) {
            return fallback;
        }
    }

    /** Only this shop's public Cloudinary images; no redirects, credentials, local addresses or arbitrary hosts. */
    boolean approved(String url) {
        try {
            var uri = URI.create(url);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && "res.cloudinary.com".equalsIgnoreCase(uri.getHost())
                    && uri.getUserInfo() == null
                    && uri.getFragment() == null
                    && (uri.getPort() == -1 || uri.getPort() == 443)
                    && !cloud.isBlank()
                    && uri.getPath().startsWith(
                    "/" + cloud + "/image/upload/"
            );
        } catch (Exception invalid) {
            return false;
        }
    }

    private Map<String, String> download(String url) throws Exception {
        if (!approved(url)) {
            return null;
        }

        var uri = URI.create(
                url.replace(
                        "/image/upload/",
                        "/image/upload/c_limit,w_640,h_640/"
                )
        );
        var response = http.send(
                HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofSeconds(3))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofInputStream()
        );
        byte[] bytes;

        try (var input = response.body()) {
            bytes = input.readNBytes(524289);
        }

        String type = response.headers()
                .firstValue("Content-Type")
                .orElse("")
                .split(";", 2)[0];

        if (response.statusCode() != 200
                || bytes.length == 0
                || bytes.length > 524288
                || !Set.of("image/jpeg", "image/png", "image/webp")
                .contains(type)) {
            return null;
        }

        return Map.of(
                "mediaType", type,
                "imageBase64", Base64.getEncoder().encodeToString(bytes)
        );
    }
}