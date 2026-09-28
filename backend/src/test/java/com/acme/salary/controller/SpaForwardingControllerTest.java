package com.acme.salary.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The browser owns routes like /employees/7; the server has no such file. Opening one directly, or
 * refreshing on it, has to return the application shell and let it work out what to show.
 */
@WebMvcTest(SpaForwardingController.class)
class SpaForwardingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void serves_the_application_for_a_top_level_screen() throws Exception {
        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void serves_the_application_when_someone_refreshes_on_one_employee() throws Exception {
        mockMvc.perform(get("/employees/7"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void leaves_the_api_alone_so_a_wrong_path_still_answers_404() throws Exception {
        // Forwarding here would answer an API call with a page of HTML, which is far harder to
        // diagnose than a plain 404.
        mockMvc.perform(get("/api/does-not-exist")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/employees/nope")).andExpect(status().isNotFound());
    }

    @Test
    void leaves_files_alone_so_a_missing_asset_does_not_come_back_as_html() throws Exception {
        mockMvc.perform(get("/main-ABC123.js")).andExpect(status().isNotFound());
    }
}
