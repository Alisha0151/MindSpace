package com.mindspace.calendar;

import com.mindspace.auth.AuthUser;
import com.mindspace.calendar.CalendarDtos.CalendarMonth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/calendar")
@Tag(name = "Calendar")
public class CalendarController {

    private final CalendarService service;

    public CalendarController(CalendarService service) {
        this.service = service;
    }

    @Operation(summary = "Month view of check-ins and journal counts (no notes or journal text)")
    @GetMapping
    public CalendarMonth month(@AuthenticationPrincipal AuthUser user,
                               @RequestParam String month,
                               @RequestParam(required = false) Integer minMood,
                               @RequestParam(required = false) Integer maxMood,
                               @RequestParam(required = false) String emotion,
                               @RequestParam(required = false) String activity) {
        return service.month(user.userId(), month, minMood, maxMood, emotion, activity);
    }
}
