package com.fluffy.batch.annotation;

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
    String cronExpression() default "";
}
