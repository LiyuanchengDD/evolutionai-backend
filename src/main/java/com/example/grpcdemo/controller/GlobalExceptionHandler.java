package com.example.grpcdemo.controller;

import com.example.grpcdemo.controller.dto.ErrorResponse;
import com.example.grpcdemo.onboarding.OnboardingErrorCode;
import com.example.grpcdemo.onboarding.OnboardingException;
import com.example.grpcdemo.security.trial.TrialErrorCode;
import com.example.grpcdemo.security.trial.TrialException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_ARGUMENT", exception.getMessage()));
    }

    @ExceptionHandler(OnboardingException.class)
    public ResponseEntity<ErrorResponse> handleOnboardingException(OnboardingException exception) {
        OnboardingErrorCode code = exception.getErrorCode();
        String message = exception.getMessage() != null ? exception.getMessage() : code.getDefaultMessage();
        return ResponseEntity.status(code.getHttpStatus())
                .body(new ErrorResponse(code.name(), message));
    }

    @ExceptionHandler(TrialException.class)
    public ResponseEntity<ErrorResponse> handleTrialException(TrialException exception) {
        TrialErrorCode code = exception.getErrorCode();
        String message = exception.getMessage() != null ? exception.getMessage() : code.getDefaultMessage();
        return ResponseEntity.status(code.getHttpStatus())
                .body(new ErrorResponse(code.name(), message));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String message = exception.getReason() != null ? exception.getReason() : exception.getMessage();
        return ResponseEntity.status(status)
                .body(new ErrorResponse(String.valueOf(status.value()), message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception exception) {
        log.error("未处理的服务器异常", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("500", "服务器内部错误"));
    }
}
