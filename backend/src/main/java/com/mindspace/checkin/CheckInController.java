package com.mindspace.checkin;

import com.mindspace.auth.AuthUser;
import com.mindspace.checkin.CheckInDtos.CheckInRequest;
import com.mindspace.checkin.CheckInDtos.CheckInResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/checkins")
@Tag(name = "Mood check-ins", description = "One check-in per user per local calendar day.")
public class CheckInController {

    private final CheckInService service;

    public CheckInController(CheckInService service) {
        this.service = service;
    }

    @Operation(summary = "Create a check-in", description = "409 if a check-in already exists for that local date; 400 for invalid scores.")
    @PostMapping
    public ResponseEntity<CheckInResponse> create(@AuthenticationPrincipal AuthUser user, @jakarta.validation.Valid @RequestBody CheckInRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user.userId(), request));
    }

    @Operation(summary = "Today's check-in (in the user's timezone)", responses = {
            @ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "204", description = "No check-in yet today")})
    @GetMapping("/today")
    public ResponseEntity<CheckInResponse> today(@AuthenticationPrincipal AuthUser user) {
        return service.today(user.userId()).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "Check-ins in an inclusive local-date range", description = "Defaults to the last 30 days; the range may not exceed 366 days.")
    @GetMapping
    public List<CheckInResponse> list(@AuthenticationPrincipal AuthUser user,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.list(user.userId(), from, to);
    }

    @Operation(summary = "One check-in", description = "404 for ids that do not exist or belong to someone else.")
    @GetMapping("/{id}")
    public CheckInResponse get(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return service.get(user.userId(), id);
    }

    @Operation(summary = "Replace a check-in's values (the date cannot change)")
    @PutMapping("/{id}")
    public CheckInResponse update(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id, @jakarta.validation.Valid @RequestBody CheckInRequest request) {
        return service.update(user.userId(), id, request);
    }

    @Operation(summary = "Delete a check-in")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        service.delete(user.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
