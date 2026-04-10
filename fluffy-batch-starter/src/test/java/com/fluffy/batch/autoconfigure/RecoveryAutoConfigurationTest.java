package com.fluffy.batch.autoconfigure;

import com.fluffy.batch.engine.RecoveryManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(
        classes = com.fluffy.batch.TestBatchApplication.class,
        properties = "fluffy.batch.recovery.enabled=true"
)
class RecoveryAutoConfigurationTest {

    @Autowired
    private RecoveryManager recoveryManager;

    @Test
    void shouldCreateRecoveryManagerWhenEnabled() {
        assertThat(recoveryManager).isNotNull();
        assertThat(recoveryManager.getNodeId()).isNotBlank();
    }

    @Test
    void shouldNotActivateByDefault() {
        RecoveryProperties props = new RecoveryProperties();
        assertThat(props.isEnabled()).isFalse();
    }
}
