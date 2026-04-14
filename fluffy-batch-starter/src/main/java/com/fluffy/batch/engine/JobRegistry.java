package com.fluffy.batch.engine;

import com.fluffy.batch.annotation.BatchJob;
import com.fluffy.batch.api.JobHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JobRegistry implements ApplicationContextAware, InitializingBean {

    private final Map<String, JobDefinition> registry = new ConcurrentHashMap<>();
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        Map<String, Object> batchJobBeans = applicationContext.getBeansWithAnnotation(BatchJob.class);
        for (Map.Entry<String, Object> entry : batchJobBeans.entrySet()) {
            Object bean = entry.getValue();
            if (bean instanceof JobHandler handler) {
                BatchJob annotation = bean.getClass().getAnnotation(BatchJob.class);
                if (annotation == null) {
                    for (Class<?> iface : bean.getClass().getInterfaces()) {
                        annotation = iface.getAnnotation(BatchJob.class);
                        if (annotation != null) break;
                    }
                }
                if (annotation != null) {
                    JobDefinition def = JobDefinition.builder(annotation.name())
                            .description(annotation.description())
                            .maxConcurrency(annotation.maxConcurrency())
                            .async(annotation.async())
                            .timeoutSeconds(annotation.timeoutSeconds())
                            .requiredParams(annotation.requiredParams())
                            .executionMode(annotation.executionMode())
                            .handler(handler)
                            .build();
                    if (!registry.containsKey(def.name())) {
                        registry.put(def.name(), def);
                    }
                }
            }
        }
    }

    public void register(JobDefinition definition) {
        if (registry.containsKey(definition.name())) {
            throw new IllegalArgumentException("Job already registered: " + definition.name());
        }
        registry.put(definition.name(), definition);
    }

    public JobDefinition get(String name) {
        JobDefinition def = registry.get(name);
        if (def == null) {
            throw new NoSuchElementException("Job not found: " + name);
        }
        return def;
    }

    public Collection<JobDefinition> getAll() {
        return registry.values();
    }

    public boolean exists(String name) {
        return registry.containsKey(name);
    }
}
