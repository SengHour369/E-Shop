package com.example.eshop.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupRequest {
    @JsonProperty("group_code")
    @jakarta.validation.constraints.Pattern(regexp = "[A-Z0-9_-]{1,10}")
    private String groupCode;



    @NotBlank(message = "Group name cannot be blank")
    @JsonProperty("group_name")
    private String name;

    @Size(max = 500)
    private String description;

    private String status;

    private String type;

    @JsonProperty("is_active")
    private Boolean isActive = false;
}
