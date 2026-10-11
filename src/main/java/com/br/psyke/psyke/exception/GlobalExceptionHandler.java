package com.br.psyke.psyke.exception;

import com.br.psyke.psyke.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import lombok.extern.slf4j.Slf4j;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handle(InvalidCredentialsException ex, WebRequest req) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req);
    }

    @ExceptionHandler(InactiveUserException.class)
    public ResponseEntity<ErrorResponse> handle(InactiveUserException ex, WebRequest req) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handle(InvalidTokenException ex, WebRequest req) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidTransitionException.class)
    public ResponseEntity<ErrorResponse> handle(InvalidTransitionException ex, WebRequest req) {
        return build(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), req);
    }

    @ExceptionHandler(SlotConflictException.class)
    public ResponseEntity<ErrorResponse> handle(SlotConflictException ex, WebRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(TenantAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handle(TenantAlreadyExistsException ex, WebRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handle(UserAlreadyExistsException ex, WebRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handle(DuplicateResourceException ex, WebRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle(ResourceNotFoundException ex, WebRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handle(AccessDeniedException ex, WebRequest req) {
        log.warn("Access denied: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "Access denied", req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handle(MethodArgumentNotValidException ex, WebRequest req) {
        var details = ex.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage()).toList();
        return ResponseEntity.badRequest().body(new ErrorResponse(400, "Validation Error",
            "Invalid request", req.getDescription(false).replace("uri=", ""), OffsetDateTime.now(), details));
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handle(org.springframework.web.HttpRequestMethodNotSupportedException ex, WebRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), req);
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handle(org.springframework.web.HttpMediaTypeNotSupportedException ex, WebRequest req) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage(), req);
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handle(org.springframework.web.bind.MissingServletRequestParameterException ex, WebRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handle(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex, WebRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Invalid parameter: " + ex.getName(), req);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handle(org.springframework.dao.DataIntegrityViolationException ex, WebRequest req) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "Data integrity violation", req);
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handle(jakarta.validation.ConstraintViolationException ex, WebRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Invalid request parameters", req);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handle(org.springframework.http.converter.HttpMessageNotReadableException ex, WebRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request body", req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handle(IllegalArgumentException ex, WebRequest req) {
        var msg = ex.getMessage();
        if (msg == null || msg.startsWith("No enum constant") || msg.contains("UUID")) msg = "Valor inválido na requisição";
        return build(HttpStatus.BAD_REQUEST, msg, req);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handle(org.springframework.web.servlet.resource.NoResourceFoundException ex, WebRequest req) {
        return build(HttpStatus.NOT_FOUND, "Resource not found", req);
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handle(org.springframework.web.multipart.MaxUploadSizeExceededException ex, WebRequest req) {
        return build(HttpStatus.CONTENT_TOO_LARGE, "Arquivo excede o tamanho máximo permitido", req);
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handle(org.springframework.web.server.ResponseStatusException ex, WebRequest req) {
        var status = HttpStatus.valueOf(ex.getStatusCode().value());
        return build(status, ex.getReason() != null ? ex.getReason() : status.getReasonPhrase(), req);
    }

    @ExceptionHandler(org.springframework.web.method.annotation.HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handle(org.springframework.web.method.annotation.HandlerMethodValidationException ex, WebRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Invalid request parameters", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handle(Exception ex, WebRequest req) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", req);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus s, String msg, WebRequest req) {
        return ResponseEntity.status(s).body(new ErrorResponse(s.value(), s.getReasonPhrase(),
            msg, req.getDescription(false).replace("uri=", ""), OffsetDateTime.now(), List.of()));
    }
}
