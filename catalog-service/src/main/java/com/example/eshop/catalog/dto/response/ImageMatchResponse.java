package com.example.eshop.catalog.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageMatchResponse {
    private final String matchType;
    private final Double confidence;
    private final boolean exact;
    private final boolean requiresSelection;
    private final String message;
    private final String requestId;
    private final List<ImageCandidate> candidates;
}
