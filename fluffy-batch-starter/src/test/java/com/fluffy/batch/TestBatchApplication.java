package com.fluffy.batch;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;

@SpringBootApplication(scanBasePackages = "com.fluffy.batch", exclude = BatchAutoConfiguration.class)
public class TestBatchApplication {
}
