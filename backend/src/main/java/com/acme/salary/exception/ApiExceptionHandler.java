package com.acme.salary.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns the exceptions the domain throws into HTTP answers, in one place, so that controllers stay
 * about routing and the domain stays free of web concerns.
 *
 * <p>The mapping: a rule the caller broke is a 400, a state that makes the request impossible is a
 * 409, and something that is not there is a 404.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ProblemDetail handleNotFound(EmployeeNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleInvalidRequest(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    /** For example marking someone as a leaver twice: the request is fine, the state is not. */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleConflict(IllegalStateException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    /** A duplicate employee code or email reaches us as a constraint violation. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDuplicate(DataIntegrityViolationException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "That employee code or email address is already in use");
    }

    /**
     * A query parameter that breaks its own constraint, such as asking for zero results. These
     * arrive separately from request-body failures because they are checked on the method itself.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleInvalidParameter(ConstraintViolationException exception) {
        Map<String, String> errors = new HashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                errors.putIfAbsent(lastPathElement(violation.getPropertyPath().toString()), violation.getMessage()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "The request has invalid parameters");
        problem.setProperty("errors", errors);
        return problem;
    }

    private static String lastPathElement(String propertyPath) {
        int lastDot = propertyPath.lastIndexOf('.');
        return lastDot < 0 ? propertyPath : propertyPath.substring(lastDot + 1);
    }

    /**
     * Bean validation failures come back as a field-to-message map, so the UI can put each message
     * next to the input that caused it rather than showing one banner.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationFailure(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "The request has invalid fields");
        problem.setProperty("errors", errors);
        return problem;
    }
}
