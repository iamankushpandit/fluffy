package com.fluffy.batch.web;

import com.fluffy.batch.engine.JobNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleJobNotFoundException() {
        JobNotFoundException ex = new JobNotFoundException("Job not found: 42");
        ProblemDetail detail = handler.handleNotFound(ex);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(detail.getDetail()).isEqualTo("Job not found: 42");
        assertThat(detail.getTitle()).isEqualTo("Not Found");
    }

    @Test
    void shouldHandleNoSuchElementException() {
        NoSuchElementException ex = new NoSuchElementException("Job not found: test");
        ProblemDetail detail = handler.handleNotFound(ex);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(detail.getDetail()).isEqualTo("Job not found: test");
    }

    @Test
    void shouldHandleBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("Missing param: key1");
        ProblemDetail detail = handler.handleBadRequest(ex);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(detail.getDetail()).isEqualTo("Missing param: key1");
        assertThat(detail.getTitle()).isEqualTo("Bad Request");
    }

    @Test
    void shouldHandleIllegalState() {
        IllegalStateException ex = new IllegalStateException("Cannot retry running job");
        ProblemDetail detail = handler.handleIllegalState(ex);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(detail.getDetail()).isEqualTo("Cannot retry running job");
        assertThat(detail.getTitle()).isEqualTo("Conflict");
    }

    @Test
    void shouldHandleGenericException() {
        Exception ex = new Exception("Unexpected error");
        ProblemDetail detail = handler.handleGeneral(ex);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(detail.getDetail()).isEqualTo("Unexpected error");
        assertThat(detail.getTitle()).isEqualTo("Internal Server Error");
    }
}
