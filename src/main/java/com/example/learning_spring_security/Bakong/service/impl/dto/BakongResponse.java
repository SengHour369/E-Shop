package com.example.learning_spring_security.Bakong.service.impl.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BakongResponse {
    private int responseCode;
    private String responseMessage;
    private Integer errorCode;
    private Object data;
    private String status;
    private String message;
    private String qrCode;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public boolean isSuccess() {
        return responseCode == 0 || "SUCCESS".equalsIgnoreCase(status);
    }
}