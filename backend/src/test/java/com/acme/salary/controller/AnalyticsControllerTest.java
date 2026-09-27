package com.acme.salary.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.salary.dto.ExchangeRateView;
import com.acme.salary.dto.GroupSummary;
import com.acme.salary.dto.PayOutlier;
import com.acme.salary.dto.PayrollOverview;
import com.acme.salary.model.Country;
import com.acme.salary.model.Department;
import com.acme.salary.model.GroupBy;
import com.acme.salary.model.JobLevel;
import com.acme.salary.service.AnalyticsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsService service;

    @Test
    void returns_the_payroll_overview() throws Exception {
        when(service.overview()).thenReturn(new PayrollOverview(
                LocalDate.of(2025, 6, 1), "USD", 10_000L,
                new BigDecimal("812450000.00"), new BigDecimal("64000.00"), new BigDecimal("7.40")));

        mockMvc.perform(get("/api/analytics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headcount").value(10000))
                .andExpect(jsonPath("$.baseCurrency").value("USD"))
                .andExpect(jsonPath("$.totalAnnualCost").value(812450000.00))
                .andExpect(jsonPath("$.medianSalary").value(64000.00))
                .andExpect(jsonPath("$.averageIncreasePercent").value(7.40));
    }

    @Test
    void returns_a_breakdown_for_the_requested_dimension() throws Exception {
        when(service.breakdown(GroupBy.COUNTRY)).thenReturn(List.of(new GroupSummary(
                "INDIA", 3393L, new BigDecimal("120000000.00"),
                new BigDecimal("28000.00"), new BigDecimal("18000.00"), new BigDecimal("41000.00"))));

        mockMvc.perform(get("/api/analytics/breakdown").param("groupBy", "COUNTRY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].group").value("INDIA"))
                .andExpect(jsonPath("$[0].headcount").value(3393))
                .andExpect(jsonPath("$[0].lowerQuartile").value(18000.00))
                .andExpect(jsonPath("$[0].upperQuartile").value(41000.00));
    }

    @Test
    void defaults_the_breakdown_to_country() throws Exception {
        when(service.breakdown(GroupBy.COUNTRY)).thenReturn(List.of());

        mockMvc.perform(get("/api/analytics/breakdown"))
                .andExpect(status().isOk());
    }

    @Test
    void rejects_a_dimension_it_cannot_group_by() throws Exception {
        mockMvc.perform(get("/api/analytics/breakdown").param("groupBy", "FAVOURITE_COLOUR"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns_pay_outliers() throws Exception {
        when(service.outliers(anyInt())).thenReturn(List.of(new PayOutlier(
                7L, "ACME-00007", "Priya Nair", Country.INDIA, Department.ENGINEERING, JobLevel.MID,
                new BigDecimal("14000.00"), new BigDecimal("28000.00"),
                new BigDecimal("22000.00"), new BigDecimal("34000.00"), "BELOW_RANGE")));

        mockMvc.perform(get("/api/analytics/outliers").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeCode").value("ACME-00007"))
                .andExpect(jsonPath("$[0].position").value("BELOW_RANGE"))
                .andExpect(jsonPath("$[0].peerMedian").value(28000.00));
    }

    @Test
    void rejects_an_outlier_limit_that_makes_no_sense() throws Exception {
        mockMvc.perform(get("/api/analytics/outliers").param("limit", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns_the_rates_the_figures_were_converted_at() throws Exception {
        when(service.exchangeRates()).thenReturn(List.of(new ExchangeRateView("INR", new BigDecimal("83.000000"))));

        mockMvc.perform(get("/api/analytics/exchange-rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currency").value("INR"))
                .andExpect(jsonPath("$[0].unitsPerUsd").value(83.000000));
    }
}
