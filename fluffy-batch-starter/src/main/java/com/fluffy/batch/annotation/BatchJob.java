package com.fluffy.batch.annotation;

import com.fluffy.batch.engine.ExecutionMode;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface BatchJob {
    String name();
    String description() default "";
    int maxConcurrency() default 1;
    boolean async() default true;
    long timeoutSeconds() default 0;
    String[] requiredParams() default {};

    /**
     * Determines where the job is executed.
     * <ul>
     *   <li>{@code LOCAL} — runs in-process (default)</li>
     *   <li>{@code CLOUD_NATIVE} — dispatched to an external orchestrator</li>
     * </ul>
     */
    ExecutionMode executionMode() default ExecutionMode.LOCAL;
}
