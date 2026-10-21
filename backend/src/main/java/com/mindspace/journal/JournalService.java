package com.mindspace.journal;

import com.mindspace.common.DateRules;
import com.mindspace.common.FieldViolation;
import com.mindspace.common.NotFoundException;
import com.mindspace.common.PageResponse;
import com.mindspace.common.Text;
import com.mindspace.common.ValidationException;
import com.mindspace.journal.JournalDtos.JournalDetail;
import com.mindspace.journal.JournalDtos.JournalRequest;
import com.mindspace.journal.JournalDtos.JournalSummary;
import com.mindspace.user.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class JournalService {

    static final int MAX_PAGE_SIZE = 50;
    static final int MAX_TAGS = 10;
    static final int EXCERPT_LENGTH = 160;
    static final int MAX_QUERY_LENGTH = 200;

    private final JournalRepository entries;
    private final UserService users;
    private final Clock clock;

    public JournalService(JournalRepository entries, UserService users, Clock clock) {
        this.entries = entries;
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public JournalDetail create(UUID userId, JournalRequest req) {
        LocalDate today = DateRules.localDate(clock.instant(), users.zoneOf(userId));
        JournalEntry entry = new JournalEntry(userId, today);
        applyRequest(entry, req);
        return toDetail(entries.saveAndFlush(entry));
    }

    @Transactional
    public JournalDetail update(UUID userId, UUID id, JournalRequest req) {
        JournalEntry entry = entries.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Journal entry"));
        applyRequest(entry, req);
        return toDetail(entries.saveAndFlush(entry));
    }

    @Transactional(readOnly = true)
    public JournalDetail get(UUID userId, UUID id) {
        return toDetail(entries.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Journal entry")));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        if (entries.deleteByIdAndUserId(id, userId) == 0) {
            throw new NotFoundException("Journal entry");
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<JournalSummary> search(UUID userId, String q, String tag, LocalDate from, LocalDate to,
                                               int page, int size) {
        List<FieldViolation> problems = new ArrayList<>();
        if (page < 0) {
            problems.add(new FieldViolation("page", "must be 0 or greater"));
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            problems.add(new FieldViolation("size", "must be between 1 and " + MAX_PAGE_SIZE));
        }
        if (from != null && to != null && to.isBefore(from)) {
            problems.add(new FieldViolation("to", "must not be before 'from'"));
        }
        String query = Text.isBlank(q) ? null : Text.trim(q);
        if (query != null && query.length() > MAX_QUERY_LENGTH) {
            problems.add(new FieldViolation("q", "must be at most " + MAX_QUERY_LENGTH + " characters"));
        }
        String normalizedTag = null;
        if (!Text.isBlank(tag)) {
            try {
                normalizedTag = Text.normalizeTag(tag);
            } catch (IllegalArgumentException e) {
                problems.add(new FieldViolation("tag", "must be 1 to 50 characters"));
            }
        }
        ValidationException.throwIfAny(problems);
        return PageResponse.of(entries.search(userId, query, normalizedTag, from, to, PageRequest.of(page, size))
                .map(JournalService::toSummary));
    }

    private void applyRequest(JournalEntry entry, JournalRequest req) {
        List<FieldViolation> problems = new ArrayList<>();
        if (Text.isBlank(req.body())) {
            problems.add(new FieldViolation("body", "must not be blank"));
        }
        Set<String> tags = new LinkedHashSet<>();
        if (req.tags() != null) {
            for (String raw : req.tags()) {
                try {
                    tags.add(Text.normalizeTag(raw));
                } catch (IllegalArgumentException e) {
                    problems.add(new FieldViolation("tags", "each tag must be 1 to 50 characters"));
                    break;
                }
            }
            if (tags.size() > MAX_TAGS) {
                problems.add(new FieldViolation("tags", "at most " + MAX_TAGS + " distinct tags"));
            }
        }
        ValidationException.throwIfAny(problems);
        entry.apply(Text.isBlank(req.title()) ? null : Text.trim(req.title()),
                req.body(),
                Text.isBlank(req.promptText()) ? null : Text.trim(req.promptText()),
                tags);
    }

    public static String excerpt(String body) {
        String collapsed = body.replaceAll("\\s+", " ").trim();
        if (collapsed.codePointCount(0, collapsed.length()) <= EXCERPT_LENGTH) {
            return collapsed;
        }
        int end = collapsed.offsetByCodePoints(0, EXCERPT_LENGTH);
        return collapsed.substring(0, end).trim() + "…";
    }

    public static JournalSummary toSummary(JournalEntry e) {
        return new JournalSummary(e.getId(), e.getEntryDate(), e.getTitle(), excerpt(e.getBody()), sorted(e.getTags()),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    public static JournalDetail toDetail(JournalEntry e) {
        return new JournalDetail(e.getId(), e.getEntryDate(), e.getTitle(), e.getBody(), e.getPromptText(),
                sorted(e.getTags()), e.getCreatedAt(), e.getUpdatedAt());
    }

    private static List<String> sorted(Set<String> tags) {
        return tags.stream().sorted(Comparator.naturalOrder()).toList();
    }
}
