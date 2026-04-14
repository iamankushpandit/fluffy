package com.fluffy.batch.autoconfigure;

import com.fluffy.batch.engine.CronJobScheduler;
import com.fluffy.batch.web.CronScheduleController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(
        classes = com.fluffy.batch.TestBatchApplication.class,
        properties = "fluffy.batch.cron.enabled=true"
)
class CronScheduleAutoConfigurationTest {

    @Autowired
    private CronJobScheduler cronJobScheduler;

    @Autowired
    private CronScheduleController cronScheduleController;

    @Test
    void shouldCreateCronJobSchedulerWhenEnabled() {
        assertThat(cronJobScheduler).isNotNull();
        assertThat(cronJobScheduler.getNodeId()).isNotBlank();
    }

    @Test
    void shouldCreateCronScheduleControllerWhenEnabled() {
        assertThat(cronScheduleController).isNotNull();
    }

    @Test
    void shouldNotActivateByDefault() {
        CronScheduleProperties props = new CronScheduleProperties();
        assertThat(props.isEnabled()).isFalse();
    }
}
