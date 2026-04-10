package com.fluffy.batch.web;

import com.fluffy.batch.autoconfigure.ScalingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for reading and dynamically updating scaling configuration.
 * <p>
 * The POST endpoint is conditional on {@code fluffy.batch.scaling.runtime-updates-enabled=true}.
 * </p>
 */
@RestController
@RequestMapping("/api/jobs/config")
public class ScalingConfigController {

    private final ScalingProperties scalingProperties;

    public ScalingConfigController(ScalingProperties scalingProperties) {
        this.scalingProperties = scalingProperties;
    }

    @GetMapping("/scaling")
    public ResponseEntity<Map<String, Object>> getScalingConfig() {
        return ResponseEntity.ok(Map.of(
                "maxQueueDepth", scalingProperties.getMaxQueueDepth(),
                "runtimeUpdatesEnabled", scalingProperties.isRuntimeUpdatesEnabled()
        ));
    }

    @PostMapping("/scaling")
    public ResponseEntity<Map<String, Object>> updateScalingConfig(@RequestBody Map<String, Object> updates) {
        if (!scalingProperties.isRuntimeUpdatesEnabled()) {
            return ResponseEntity.status(403).body(Map.of("error", "Runtime updates are disabled"));
        }

        if (updates.containsKey("maxQueueDepth")) {
            Object value = updates.get("maxQueueDepth");
            if (value instanceof Number number) {
                scalingProperties.setMaxQueueDepth(number.intValue());
            }
        }

        return ResponseEntity.ok(Map.of(
                "maxQueueDepth", scalingProperties.getMaxQueueDepth(),
                "runtimeUpdatesEnabled", scalingProperties.isRuntimeUpdatesEnabled()
        ));
    }
}
