package com.mindspace.habit;

import com.mindspace.auth.AuthUser;
import com.mindspace.habit.HabitDtos.HabitProgress;
import com.mindspace.habit.HabitDtos.HabitRequest;
import com.mindspace.habit.HabitDtos.HabitResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/habits")
@Tag(name = "Habits", description = "Daily or weekly (Monday-Sunday) habits with separate completion history, pauses and streaks.")
public class HabitController {

    private final HabitService service;

    public HabitController(HabitService service) {
        this.service = service;
    }

    @Operation(summary = "List habits with streaks and recent history")
    @GetMapping
    public List<HabitResponse> list(@AuthenticationPrincipal AuthUser user) {
        return service.list(user.userId());
    }

    @Operation(summary = "Create a habit", description = "DAILY needs targetCount 1 (default); WEEKLY needs 1..7. 409 if the name is already used.")
    @PostMapping
    public ResponseEntity<HabitResponse> create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody HabitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user.userId(), request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "One habit")
    public HabitResponse get(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return service.get(user.userId(), id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a habit's definition (history is kept)")
    public HabitResponse update(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id, @Valid @RequestBody HabitRequest request) {
        return service.update(user.userId(), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a habit and its history")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        service.delete(user.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "Pause from today (paused days neither count nor break a streak)")
    public HabitResponse pause(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return service.pause(user.userId(), id);
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "Resume from today")
    public HabitResponse resume(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return service.resume(user.userId(), id);
    }

    @PutMapping("/{id}/completions/{date}")
    @Operation(summary = "Mark a day complete (idempotent)", description = "400 if the date is in the future, before the start, after the end, or during a pause.")
    public ResponseEntity<Void> complete(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                                         @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        service.complete(user.userId(), id, date);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/completions/{date}")
    @Operation(summary = "Undo a completion (idempotent)")
    public ResponseEntity<Void> uncomplete(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                                           @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        service.uncomplete(user.userId(), id, date);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/progress")
    @Operation(summary = "Completions, weekly progress, consistency and streaks for a range", description = "Defaults to the last 28 days; at most 366 days.")
    public HabitProgress progress(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.progress(user.userId(), id, from, to);
    }
}
