package com.acme.salary.controller;

import com.acme.salary.dto.ExchangeRateView;
import com.acme.salary.dto.GroupSummary;
import com.acme.salary.dto.PayOutlier;
import com.acme.salary.dto.PayrollOverview;
import com.acme.salary.model.GroupBy;
import com.acme.salary.service.AnalyticsService;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Validated
public class AnalyticsController {

    private final AnalyticsService service;

    @GetMapping("/overview")
    public PayrollOverview overview() {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @GetMapping("/breakdown")
    public List<GroupSummary> breakdown(@RequestParam(defaultValue = "COUNTRY") GroupBy groupBy) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @GetMapping("/outliers")
    public List<PayOutlier> outliers(@RequestParam(defaultValue = "10") @Min(1) int limit) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @GetMapping("/exchange-rates")
    public List<ExchangeRateView> exchangeRates() {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
