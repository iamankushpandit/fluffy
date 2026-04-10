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
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.fluffy.batch.TestBatchApplication.class)
@AutoConfigureMockMvc
class JobControllerEdgeCaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRegistry jobRegistry;

    @BeforeEach
    void setUp() {
        String jobName = "edge-case-job";
        if (!jobRegistry.exists(jobName)) {
            JobDefinition def = JobDefinition.builder(jobName)
                    .async(false)
                    .handler(ctx -> {})
                    .build();
            jobRegistry.register(def);
        }

        String failingJobName = "failing-job";
        if (!jobRegistry.exists(failingJobName)) {
            JobDefinition def = JobDefinition.builder(failingJobName)
                    .async(false)
                    .handler(ctx -> { throw new RuntimeException("simulated failure"); })
                    .build();
            jobRegistry.register(def);
        }
    }

    @Test
    void shouldStartJobWithoutBody() throws Exception {
        mockMvc.perform(post("/api/jobs/edge-case-job/start")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobName").value("edge-case-job"))
                .andExpect(jsonPath("$.requestedBy").value("anonymous"));
    }

    @Test
    void shouldStartJobWithXUserIdHeader() throws Exception {
        mockMvc.perform(post("/api/jobs/edge-case-job/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("X-User-Id", "custom-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedBy").value("custom-user"));
    }

    @Test
    void shouldStartJobWithRequestedByInBody() throws Exception {
        mockMvc.perform(post("/api/jobs/edge-case-job/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestedBy\":\"body-user\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedBy").value("body-user"));
    }

    @Test
    void shouldReturnNotFoundForUnknownExecutionStatus() throws Exception {
        mockMvc.perform(get("/api/jobs/999999/status"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldStopJob() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/jobs/edge-case-job/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn();

        String jobId = extractJobId(result.getResponse().getContentAsString());

        mockMvc.perform(post("/api/jobs/" + jobId + "/stop"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRetryCompletedJob() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/jobs/edge-case-job/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn();

        String jobId = extractJobId(result.getResponse().getContentAsString());

        mockMvc.perform(post("/api/jobs/" + jobId + "/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobName").value("edge-case-job"));
    }

    @Test
    void shouldReturnNotFoundForUnknownRetry() throws Exception {
        mockMvc.perform(post("/api/jobs/999999/retry"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldHandleFailedJob() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/jobs/failing-job/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn();

        String jobId = extractJobId(result.getResponse().getContentAsString());

        mockMvc.perform(get("/api/jobs/" + jobId + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").value("simulated failure"));
    }

    @Test
    void shouldRetryFailedJob() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/jobs/failing-job/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn();

        String jobId = extractJobId(result.getResponse().getContentAsString());

        mockMvc.perform(post("/api/jobs/" + jobId + "/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobName").value("failing-job"));
    }

    private String extractJobId(String json) {
        int start = json.indexOf("\"jobId\":") + 8;
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        return json.substring(start, end).trim();
    }
}
