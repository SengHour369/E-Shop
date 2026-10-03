package com.example.eshop.common.exception;

import com.example.eshop.common.dto.APIResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@lombok.RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final com.example.eshop.common.audit.AuditLogService audit;
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<APIResponse<Object>> handleDenied(Exception ex) {
        audit.recordSecurity(com.example.eshop.common.audit.AuditAction.ACCESS_DENIED,
            com.example.eshop.common.audit.AuditResult.DENIED,
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication(), "FORBIDDEN");
        return ResponseEntity.status(403).body(APIResponse.error("Forbidden", 403));
    }

    @ExceptionHandler({org.springframework.validation.BindException.class,
        org.springframework.http.converter.HttpMessageNotReadableException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public ResponseEntity<APIResponse<Object>> handleValidation(Exception ex) {
        return ResponseEntity.badRequest().body(APIResponse.error("Invalid request: check required fields, values and formats", 400));
    }

    @ExceptionHandler({org.springframework.dao.DataIntegrityViolationException.class,
        org.springframework.dao.OptimisticLockingFailureException.class,
        org.springframework.dao.PessimisticLockingFailureException.class})
    public ResponseEntity<APIResponse<Object>> handleConflict(Exception ex) {
        return ResponseEntity.status(409).body(APIResponse.error("Conflicting data or concurrent update; retry the operation", 409));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIResponse<Object>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(APIResponse.error(ex.getMessage(), HttpStatus.NOT_FOUND.value()));
    }

    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<APIResponse<Object>> handleBusinessLogic(BusinessLogicException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(APIResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(CustomMessageException.class)
    public ResponseEntity<APIResponse<Object>> handleCustomMessage(CustomMessageException ex) {
        int status;
        try {
            status = Integer.parseInt(ex.getCode());
        } catch (NumberFormatException e) {
            status = HttpStatus.BAD_REQUEST.value();
        }
        return ResponseEntity.status(status)
                .body(APIResponse.error(ex.getMessage(), status));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse<Object>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(APIResponse.error("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }
}
