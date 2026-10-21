package com.mindspace.journal;

import com.mindspace.auth.AuthUser;
import com.mindspace.common.PageResponse;
import com.mindspace.journal.JournalDtos.JournalDetail;
import com.mindspace.journal.JournalDtos.JournalRequest;
import com.mindspace.journal.JournalDtos.JournalSummary;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/journal")
@Tag(name = "Private journal", description = "Plain-text entries, private to the signed-in user. Search and filters are always scoped to that user.")
public class JournalController {

    private final JournalService service;

    public JournalController(JournalService service) {
        this.service = service;
    }

    @Operation(summary = "Create an entry (dated today in the user's timezone)")
    @PostMapping
    public ResponseEntity<JournalDetail> create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody JournalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user.userId(), request));
    }

    @Operation(summary = "Search and filter entries, newest first",
            description = "q: full-text search (title and body). tag: exact tag. from/to: inclusive entry dates. size 1..50. Results contain excerpts, not full text.")
    @GetMapping
    public PageResponse<JournalSummary> search(@AuthenticationPrincipal AuthUser user,
                                               @RequestParam(required = false) String q,
                                               @RequestParam(required = false) String tag,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return service.search(user.userId(), q, tag, from, to, page, size);
    }

    @Operation(summary = "One entry with full text", description = "404 for ids that do not exist or belong to someone else.")
    @GetMapping("/{id}")
    public JournalDetail get(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return service.get(user.userId(), id);
    }

    @Operation(summary = "Replace an entry's title, text, prompt and tags")
    @PutMapping("/{id}")
    public JournalDetail update(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                                @Valid @RequestBody JournalRequest request) {
        return service.update(user.userId(), id, request);
    }

    @Operation(summary = "Permanently delete an entry")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        service.delete(user.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
