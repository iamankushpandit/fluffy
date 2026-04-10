package com.fluffy.batch.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BatchJobParam {
    String name();
    boolean required() default false;
    String description() default "";
}
