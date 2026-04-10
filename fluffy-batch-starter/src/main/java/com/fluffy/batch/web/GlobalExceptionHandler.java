package com.fluffy.batch.web;

import com.fluffy.batch.engine.JobNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.net.URI;
import java.util.NoSuchElementException;

/**
 * Unified error handling using RFC 7807 {@link ProblemDetail} responses.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({JobNotFoundException.class, NoSuchElementException.class})
    public ProblemDetail handleNotFound(RuntimeException ex) {
        return buildProblemDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException ex) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        return buildProblemDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex) {
        return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    private ProblemDetail buildProblemDetail(HttpStatus status, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setType(URI.create("about:blank"));
        detail.setTitle(status.getReasonPhrase());
        return detail;
    }
}
