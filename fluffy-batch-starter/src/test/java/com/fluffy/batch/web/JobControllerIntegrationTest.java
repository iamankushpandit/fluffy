package com.fluffy.batch.web;

import com.fluffy.batch.engine.JobDefinition;
import com.fluffy.batch.engine.JobRegistry;
import org.junit.jupiter.api.BeforeEach;
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
class JobControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRegistry jobRegistry;

    @BeforeEach
    void setUp() {
        String jobName = "integration-test-job";
        if (!jobRegistry.exists(jobName)) {
            JobDefinition def = JobDefinition.builder(jobName)
                    .async(false)
                    .handler(ctx -> {})
                    .build();
            jobRegistry.register(def);
        }
    }

    @Test
    void shouldStartJob() throws Exception {
        mockMvc.perform(post("/api/jobs/integration-test-job/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("X-User-Id", "test-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobName").value("integration-test-job"))
                .andExpect(jsonPath("$.jobId").isNumber());
    }

    @Test
    void shouldReturnNotFoundForUnknownJob() throws Exception {
        mockMvc.perform(post("/api/jobs/unknown-job-xyz/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Test
    void shouldGetJobStatus() throws Exception {
        String result = mockMvc.perform(post("/api/jobs/integration-test-job/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int start = result.indexOf("\"jobId\":") + 8;
        int end = result.indexOf(",", start);
        if (end == -1) end = result.indexOf("}", start);
        String jobId = result.substring(start, end).trim();

        mockMvc.perform(get("/api/jobs/" + jobId + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(Long.parseLong(jobId)));
    }

    @Test
    void shouldListRegisteredJobs() throws Exception {
        mockMvc.perform(get("/api/jobs/registered"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldListExecutions() throws Exception {
        mockMvc.perform(get("/api/jobs/executions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldReturnBadRequestForMissingRequiredParam() throws Exception {
        String jobName = "integration-required-param-job";
        if (!jobRegistry.exists(jobName)) {
            JobDefinition def = JobDefinition.builder(jobName)
                    .async(false)
                    .requiredParams("requiredKey")
                    .handler(ctx -> {})
                    .build();
            jobRegistry.register(def);
        }

        mockMvc.perform(post("/api/jobs/" + jobName + "/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }
}
