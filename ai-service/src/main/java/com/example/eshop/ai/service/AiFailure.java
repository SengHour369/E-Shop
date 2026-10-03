package com.example.eshop.ai.service;
import com.example.eshop.ai.enums.AiExecutionStatus;
public class AiFailure extends RuntimeException {
    private final String code;
    private final AiExecutionStatus status;
    public AiFailure(String code, AiExecutionStatus status, String safeMessage) {
        super(safeMessage); this.code = code; this.status = status;
    }
    public String code() { return code; }
    public AiExecutionStatus status() { return status; }
}
