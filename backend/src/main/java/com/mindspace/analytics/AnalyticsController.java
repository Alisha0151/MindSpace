package com.mindspace.analytics;

import com.mindspace.analytics.AnalyticsDtos.AnalyticsSummary;
import com.mindspace.auth.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "Observed summaries of the user's own check-ins. Not diagnostic.")
public class AnalyticsController {

    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    @Operation(summary = "Mood trends, distribution, sleep and habit comparisons for a range (default last 30 days, max 366)")
    @GetMapping("/summary")
    public AnalyticsSummary summary(
            @AuthenticationPrincipal com.mindspace.auth.AuthUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate compareFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate compareTo,
            @RequestParam(required = false) UUID habitId) {
        return service.summary(user.userId(), from, to, compareFrom, compareTo, habitId);
    }
}
