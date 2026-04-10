package com.fluffy.batch.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.fluffy.batch.TestBatchApplication.class)
@AutoConfigureMockMvc
class DashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldServeDashboardIndexHtml() throws Exception {
        mockMvc.perform(get("/fluffy-dashboard/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    void shouldServeDashboardCss() throws Exception {
        mockMvc.perform(get("/fluffy-dashboard/css/dashboard.css"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldServeDashboardJs() throws Exception {
        mockMvc.perform(get("/fluffy-dashboard/js/api-client.js"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/fluffy-dashboard/js/state.js"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/fluffy-dashboard/js/render.js"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/fluffy-dashboard/js/events.js"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/fluffy-dashboard/js/app.js"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnDashboardConfig() throws Exception {
        mockMvc.perform(get("/api/jobs/dashboard/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Fluffy Batch Dashboard"))
                .andExpect(jsonPath("$.refreshInterval").value(5))
                .andExpect(jsonPath("$.authEnabled").value(false));
    }
}
