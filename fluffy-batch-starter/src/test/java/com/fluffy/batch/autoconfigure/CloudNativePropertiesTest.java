package com.fluffy.batch.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CloudNativePropertiesTest {

    @Test
    void shouldHaveDefaultValues() {
        CloudNativeProperties props = new CloudNativeProperties();

        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getEndpoint()).isNull();
        assertThat(props.getCallbackUrl()).isNull();
        assertThat(props.getConnectTimeoutSeconds()).isEqualTo(10);
        assertThat(props.getRequestTimeoutSeconds()).isEqualTo(30);
    }

    @Test
    void shouldSetAndGetEnabled() {
        CloudNativeProperties props = new CloudNativeProperties();
        props.setEnabled(true);
        assertThat(props.isEnabled()).isTrue();
    }

    @Test
    void shouldSetAndGetEndpoint() {
        CloudNativeProperties props = new CloudNativeProperties();
        props.setEndpoint("http://example.com/api");
        assertThat(props.getEndpoint()).isEqualTo("http://example.com/api");
    }

    @Test
    void shouldSetAndGetCallbackUrl() {
        CloudNativeProperties props = new CloudNativeProperties();
        props.setCallbackUrl("http://myapp/callback");
        assertThat(props.getCallbackUrl()).isEqualTo("http://myapp/callback");
    }

    @Test
    void shouldSetAndGetConnectTimeoutSeconds() {
        CloudNativeProperties props = new CloudNativeProperties();
        props.setConnectTimeoutSeconds(20);
        assertThat(props.getConnectTimeoutSeconds()).isEqualTo(20);
    }

    @Test
    void shouldSetAndGetRequestTimeoutSeconds() {
        CloudNativeProperties props = new CloudNativeProperties();
        props.setRequestTimeoutSeconds(60);
        assertThat(props.getRequestTimeoutSeconds()).isEqualTo(60);
    }

    @Test
    void shouldAllowZeroTimeouts() {
        CloudNativeProperties props = new CloudNativeProperties();
        props.setConnectTimeoutSeconds(0);
        props.setRequestTimeoutSeconds(0);
        assertThat(props.getConnectTimeoutSeconds()).isEqualTo(0);
        assertThat(props.getRequestTimeoutSeconds()).isEqualTo(0);
    }

    @Test
    void shouldAllowNullEndpointAndCallback() {
        CloudNativeProperties props = new CloudNativeProperties();
        props.setEndpoint(null);
        props.setCallbackUrl(null);
        assertThat(props.getEndpoint()).isNull();
        assertThat(props.getCallbackUrl()).isNull();
    }
}
