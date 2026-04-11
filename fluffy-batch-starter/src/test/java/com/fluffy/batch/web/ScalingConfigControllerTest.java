package com.fluffy.batch.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.fluffy.batch.TestBatchApplication.class)
@AutoConfigureMockMvc
class ScalingConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGetScalingConfig() throws Exception {
        mockMvc.perform(get("/api/jobs/config/scaling"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxQueueDepth").isNumber())
                .andExpect(jsonPath("$.runtimeUpdatesEnabled").isBoolean());
    }

    @Test
    void shouldUpdateScalingConfig() throws Exception {
        mockMvc.perform(post("/api/jobs/config/scaling")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxQueueDepth\": 200}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxQueueDepth").value(200));
    }

    @Test
    void shouldHandleEmptyUpdate() throws Exception {
        mockMvc.perform(post("/api/jobs/config/scaling")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }
}
