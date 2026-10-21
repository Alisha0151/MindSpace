package com.mindspace.checkin;

import com.mindspace.checkin.CheckInDtos.CheckInRequest;
import com.mindspace.checkin.CheckInDtos.CheckInResponse;
import com.mindspace.checkin.domain.CheckInRules;
import com.mindspace.common.ConflictException;
import com.mindspace.common.DateRange;
import com.mindspace.common.DateRules;
import com.mindspace.common.FieldViolation;
import com.mindspace.common.NotFoundException;
import com.mindspace.common.Text;
import com.mindspace.common.ValidationException;
import com.mindspace.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Policy: at most one check-in per user per LOCAL calendar day (users.timezone at the time of creation).
 * The unique constraint (user_id, local_date) is the authority; the pre-check only gives a friendlier message.
 */
@Service
public class CheckInService {

    static final int MAX_TAGS_PER_TYPE = 10;
    static final int MAX_RANGE_DAYS = 366;
    private static final LocalDate EARLIEST = LocalDate.of(2000, 1, 1);

    private final CheckInRepository checkIns;
    private final UserService users;
    private final Clock clock;

    public CheckInService(CheckInRepository checkIns, UserService users, Clock clock) {
        this.checkIns = checkIns;
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public CheckInResponse create(UUID userId, CheckInRequest req) {
        ZoneId zone = users.zoneOf(userId);
        LocalDate today = DateRules.localDate(clock.instant(), zone);
        LocalDate date = req.date() != null ? req.date() : today;

        List<FieldViolation> problems = new ArrayList<>();
        if (date.isAfter(today)) {
            problems.add(new FieldViolation("date", "cannot be in the future"));
        } else if (date.isBefore(EARLIEST)) {
            problems.add(new FieldViolation("date", "is too far in the past"));
        }
        Set<CheckInTag> tags = collectTags(req, problems);
        problems.addAll(CheckInRules.validate(req.moodScore(), req.stressScore(), req.energyScore(),
                req.sleepMinutes(), req.sleepQuality(), req.notes()));
        ValidationException.throwIfAny(problems);

        if (checkIns.existsByUserIdAndLocalDate(userId, date)) {
            throw new ConflictException("A check-in already exists for that date");
        }
        MoodCheckIn entity = new MoodCheckIn(userId, date, zone.getId());
        entity.apply(req.moodScore(), req.stressScore(), req.energyScore(), req.sleepMinutes(), req.sleepQuality(),
                blankToNull(req.notes()), tags);
        // saveAndFlush so a concurrent duplicate fails here (unique violation -> 409) rather than at commit.
        return toResponse(checkIns.saveAndFlush(entity));
    }

    @Transactional
    public CheckInResponse update(UUID userId, UUID id, CheckInRequest req) {
        MoodCheckIn entity = checkIns.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Check-in"));
        List<FieldViolation> problems = new ArrayList<>();
        Set<CheckInTag> tags = collectTags(req, problems);
        problems.addAll(CheckInRules.validate(req.moodScore(), req.stressScore(), req.energyScore(),
                req.sleepMinutes(), req.sleepQuality(), req.notes()));
        ValidationException.throwIfAny(problems);
        entity.apply(req.moodScore(), req.stressScore(), req.energyScore(), req.sleepMinutes(), req.sleepQuality(),
                blankToNull(req.notes()), tags);
        return toResponse(checkIns.saveAndFlush(entity));
    }

    @Transactional(readOnly = true)
    public CheckInResponse get(UUID userId, UUID id) {
        return toResponse(checkIns.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Check-in")));
    }

    @Transactional(readOnly = true)
    public Optional<CheckInResponse> today(UUID userId) {
        LocalDate today = DateRules.localDate(clock.instant(), users.zoneOf(userId));
        return checkIns.findByUserIdAndLocalDate(userId, today).map(CheckInService::toResponse);
    }

    @Transactional(readOnly = true)
    public List<CheckInResponse> list(UUID userId, LocalDate from, LocalDate to) {
        LocalDate today = DateRules.localDate(clock.instant(), users.zoneOf(userId));
        DateRange range = DateRange.resolve(from, to, today, 30, MAX_RANGE_DAYS);
        return checkIns.findByUserIdAndLocalDateBetweenOrderByLocalDateAsc(userId, range.from(), range.to()).stream()
                .map(CheckInService::toResponse).toList();
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        if (checkIns.deleteByIdAndUserId(id, userId) == 0) {
            throw new NotFoundException("Check-in");
        }
    }

    private static Set<CheckInTag> collectTags(CheckInRequest req, List<FieldViolation> problems) {
        Set<CheckInTag> tags = new HashSet<>();
        addTags(tags, TagType.EMOTION, "emotions", req.emotions(), problems);
        addTags(tags, TagType.ACTIVITY, "activities", req.activities(), problems);
        addTags(tags, TagType.CONTEXT, "contexts", req.contexts(), problems);
        return tags;
    }

    private static void addTags(Set<CheckInTag> out, TagType type, String field, List<String> raw, List<FieldViolation> problems) {
        if (raw == null) {
            return;
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : raw) {
            try {
                normalized.add(Text.normalizeTag(value));
            } catch (IllegalArgumentException e) {
                problems.add(new FieldViolation(field, "each tag must be 1 to 50 characters"));
                return;
            }
        }
        if (normalized.size() > MAX_TAGS_PER_TYPE) {
            problems.add(new FieldViolation(field, "at most " + MAX_TAGS_PER_TYPE + " distinct tags"));
            return;
        }
        normalized.forEach(label -> out.add(new CheckInTag(type, label)));
    }

    private static String blankToNull(String value) {
        return Text.isBlank(value) ? null : value;
    }

    public static CheckInResponse toResponse(MoodCheckIn c) {
        return new CheckInResponse(c.getId(), c.getLocalDate(), c.getTimezone(), c.getMoodScore(), c.getStressScore(),
                c.getEnergyScore(), c.getSleepMinutes(), c.getSleepQuality(),
                labels(c, TagType.EMOTION), labels(c, TagType.ACTIVITY), labels(c, TagType.CONTEXT),
                c.getNotes(), c.getCreatedAt(), c.getUpdatedAt());
    }

    static List<String> labels(MoodCheckIn c, TagType type) {
        return c.getTags().stream().filter(t -> t.getTagType() == type)
                .map(CheckInTag::getLabel).sorted(Comparator.naturalOrder()).toList();
    }
}
