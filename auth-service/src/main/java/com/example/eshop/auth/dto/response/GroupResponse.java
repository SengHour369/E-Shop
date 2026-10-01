package com.example.eshop.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponse {

    @JsonProperty("group_id")
    private Long groupId;

    @JsonProperty("group_code")
    private String groupCode;

    @JsonProperty("group_name")
    private String groupName;

    private String description;
    private String status;
    private String type;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}