package com.enterprise.cyclone.adapter.in.web;

import com.enterprise.cyclone.application.AssetNotFoundException;
import com.enterprise.cyclone.web.CorrelationId;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * Translates failures into RFC 9457 problem responses.
 *
 * <p>Three rules hold for every response produced here. Nothing is leaked: a client never sees a
 * stack trace, a class name or a SQL fragment. Every response carries the correlation id, so a
 * client report maps onto exactly one log line. And failures are classified honestly &mdash; a bad
 * value is a 400, an absent asset is a 404, a bad credential is a 401, an unexpected fault is a 500
 * that is logged in full on the server and described vaguely to the caller.
 *
 * <p>The domain signals contract violations with {@link IllegalArgumentException} and a message that
 * already names the offending field and its accepted range, so that message is passed through: it is
 * the difference between an integrator fixing a payload in a minute and opening a support ticket.
 *
 * <p>Ordered ahead of Spring's built-in {@code ProblemDetailsExceptionHandler}. Both advices can
 * handle a validation failure, and with equal precedence the framework's wins, which silently takes
 * the field-level {@code errors} array and the correlation id out of every 400 the console needs to
 * explain to a user. Declaring precedence here is what makes the documented error contract real.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
final class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String UNSPECIFIED = "no message provided";

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail onDomainViolation(IllegalArgumentException exception, HttpServletRequest request) {
        return problem(request, HttpStatus.BAD_REQUEST, "Request violates a domain contract", exception.getMessage());
    }

    @ExceptionHandler(AssetNotFoundException.class)
    ProblemDetail onAssetNotFound(AssetNotFoundException exception, HttpServletRequest request) {
        return problem(request, HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail onBadCredentials(BadCredentialsException exception, HttpServletRequest request) {
        return problem(request, HttpStatus.UNAUTHORIZED, "Authentication failed",
                "Invalid username or password");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail onValidationFailure(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail problem = problem(request, HttpStatus.BAD_REQUEST, "Request payload is invalid",
                "One or more fields are missing or outside the accepted range");
        problem.setProperty("errors", fieldErrors(exception));
        return problem;
    }

    /**
     * Constraint violations on path variables and request parameters, which are validated on the
     * method invocation rather than on a deserialised body.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    ProblemDetail onParameterValidationFailure(HandlerMethodValidationException exception, HttpServletRequest request) {
        return problem(request, HttpStatus.BAD_REQUEST, "Request parameter is invalid",
                "One or more path or query values are missing or outside the accepted range");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail onUnreadableBody(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return problem(request, HttpStatus.BAD_REQUEST, "Request body could not be read",
                "Body must be valid JSON, well within the accepted nesting and size limits, with timestamps in "
                        + "RFC 3339 form such as 2026-09-27T06:00:00Z");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail onUnexpectedFailure(Exception exception, HttpServletRequest request) {
        LOG.error("Unhandled failure while serving {}", request.getRequestURI(), exception);
        return problem(request, HttpStatus.INTERNAL_SERVER_ERROR, "Request could not be completed",
                "The request was accepted but could not be completed; quote the correlation id when reporting this");
    }

    private static List<Map<String, String>> fieldErrors(MethodArgumentNotValidException exception) {
        return exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", Objects.requireNonNullElse(error.getDefaultMessage(), UNSPECIFIED)))
                .toList();
    }

    private static ProblemDetail problem(HttpServletRequest request, HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, Objects.requireNonNullElse(detail, UNSPECIFIED));
        problem.setTitle(title);
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        String correlationId = CorrelationId.current(request);
        if (correlationId != null) {
            problem.setProperty("correlationId", correlationId);
        }
        return problem;
    }
}
