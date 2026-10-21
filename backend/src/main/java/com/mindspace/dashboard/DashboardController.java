package com.mindspace.dashboard;

import com.mindspace.auth.AuthUser;
import com.mindspace.dashboard.DashboardDtos.Dashboard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @Operation(summary = "Home-screen aggregate: today's check-in, 7-day mood, habits, recent journal summaries")
    @GetMapping
    public Dashboard dashboard(@AuthenticationPrincipal AuthUser user) {
        return service.load(user.userId());
    }
}
