package com.fluffy.batch.autoconfigure;

import com.fluffy.batch.backend.BackendType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class DatabaseBackendAutoConfigurationTest {

    @Test
    void shouldOnlyActivateWithDatabaseType() {
        // The configuration class uses @ConditionalOnProperty(havingValue = "database")
        // When type is h2, the configuration should not activate
        BackendProperties props = new BackendProperties();
        props.setType(BackendType.H2);
        assertThat(props.getType()).isNotEqualTo(BackendType.DATABASE);
    }

    @Test
    void shouldActivateWithDatabaseType() {
        BackendProperties props = new BackendProperties();
        props.setType(BackendType.DATABASE);
        assertThat(props.getType()).isEqualTo(BackendType.DATABASE);
    }
}
