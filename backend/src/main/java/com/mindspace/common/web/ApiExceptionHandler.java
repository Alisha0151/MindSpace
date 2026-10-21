package com.mindspace.common.web;

import com.mindspace.auth.AuthenticationFailedException;
import com.mindspace.common.ConflictException;
import com.mindspace.common.FieldViolation;
import com.mindspace.common.NotFoundException;
import com.mindspace.common.RateLimitedException;
import com.mindspace.common.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Maps every failure to an RFC 9457 problem document. Responses never contain stack traces, SQL,
 * secrets or submitted free text. The request id (also in the logs) lets a user report a problem.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Database constraint name -> user-facing conflict message. */
    private static final Map<String, String> CONSTRAINT_MESSAGES = Map.of(
            "mood_check_ins_user_date_key", "A check-in already exists for that date",
            "habits_user_name_uq", "You already have a habit with that name",
            "users_email_key", "An account with this e-mail already exists",
            "habit_completions_habit_date_key", "That completion is already recorded");

    @ExceptionHandler(ValidationException.class)
    ResponseEntity<ProblemDetail> validation(ValidationException ex) {
        return problem(HttpStatus.BAD_REQUEST, "validation-failed", "Validation failed",
                "One or more fields are invalid.", ex.violations());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> beanValidation(MethodArgumentNotValidException ex) {
        List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldViolation(e.getField(), e.getDefaultMessage()))
                .toList();
        return problem(HttpStatus.BAD_REQUEST, "validation-failed", "Validation failed",
                "One or more fields are invalid.", violations);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> constraintViolation(ConstraintViolationException ex) {
        List<FieldViolation> violations = ex.getConstraintViolations().stream()
                .map(v -> new FieldViolation(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return problem(HttpStatus.BAD_REQUEST, "validation-failed", "Validation failed",
                "One or more parameters are invalid.", violations);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<ProblemDetail> malformed(Exception ex) {
        return problem(HttpStatus.BAD_REQUEST, "malformed-request", "Malformed request",
                "The request could not be read. Check the format of the body and parameters.", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> methodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "method-not-allowed", "Method not allowed", null, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ProblemDetail> unsupportedMedia(HttpMediaTypeNotSupportedException ex) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported-media-type", "Unsupported media type",
                "Send the body as application/json.", null);
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "not-found", "Not found", ex.getMessage(), null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ProblemDetail> noResource(NoResourceFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "not-found", "Not found", "No such endpoint.", null);
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ProblemDetail> conflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, "conflict", "Conflict", ex.getMessage(), null);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    ResponseEntity<ProblemDetail> authFailed(AuthenticationFailedException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "authentication-failed", "Authentication failed",
                "The credentials or session are not valid.", null);
    }

    @ExceptionHandler(RateLimitedException.class)
    ResponseEntity<ProblemDetail> rateLimited(RateLimitedException ex) {
        ResponseEntity<ProblemDetail> base = problem(HttpStatus.TOO_MANY_REQUESTS, "rate-limited", "Too many requests",
                "Too many attempts. Please wait before trying again.", null);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.retryAfterSeconds()))
                .body(base.getBody());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> optimisticLock(OptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "concurrent-modification", "Conflict",
                "This record was changed by another request. Reload and try again.", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> integrity(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause() == null ? "" : String.valueOf(ex.getMostSpecificCause().getMessage());
        for (Map.Entry<String, String> known : CONSTRAINT_MESSAGES.entrySet()) {
            if (message.contains(known.getKey())) {
                return problem(HttpStatus.CONFLICT, "conflict", "Conflict", known.getValue(), null);
            }
        }
        log.warn("Unmapped data integrity violation (requestId={})", MDC.get("requestId"));
        return problem(HttpStatus.CONFLICT, "conflict", "Conflict", "The change conflicts with existing data.", null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception ex, HttpServletRequest request) {
        // Log the exception type and stack, never the request body or query string.
        log.error("Unhandled {} on {} {} (requestId={})", ex.getClass().getName(),
                request.getMethod(), request.getRequestURI(), MDC.get("requestId"), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Unexpected error",
                "Something went wrong. If it keeps happening, quote the request id.", null);
    }

    static ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String title,
                                                 String detail, List<FieldViolation> errors) {
        ProblemDetail body = ProblemDetail.forStatus(status);
        body.setType(URI.create("urn:mindspace:problem:" + code));
        body.setTitle(title);
        if (detail != null) {
            body.setDetail(detail);
        }
        String requestId = MDC.get("requestId");
        if (requestId != null) {
            body.setProperty("requestId", requestId);
        }
        if (errors != null && !errors.isEmpty()) {
            body.setProperty("errors", errors);
        }
        return ResponseEntity.status(status).contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }
}
