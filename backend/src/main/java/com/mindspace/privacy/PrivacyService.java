package com.mindspace.privacy;

import com.mindspace.checkin.CheckInRepository;
import com.mindspace.checkin.CheckInService;
import com.mindspace.common.NotFoundException;
import com.mindspace.common.ValidationException;
import com.mindspace.habit.Habit;
import com.mindspace.habit.HabitCompletion;
import com.mindspace.habit.HabitCompletionRepository;
import com.mindspace.habit.HabitPause;
import com.mindspace.habit.HabitPauseRepository;
import com.mindspace.habit.HabitRepository;
import com.mindspace.journal.JournalRepository;
import com.mindspace.journal.JournalService;
import com.mindspace.privacy.PrivacyDtos.ExportConsent;
import com.mindspace.privacy.PrivacyDtos.ExportDocument;
import com.mindspace.privacy.PrivacyDtos.ExportHabit;
import com.mindspace.privacy.PrivacyDtos.ExportPause;
import com.mindspace.privacy.PrivacyDtos.ExportProfile;
import com.mindspace.user.ConsentRecordRepository;
import com.mindspace.user.UserDtos.UserResponse;
import com.mindspace.user.UserRepository;
import com.mindspace.user.UserService;
import com.mindspace.wellness.CopingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Export and deletion. The export is assembled in memory, which is fine for the data volumes of a single
 * personal account (documented limitation). Deletion is a single user-row delete; every owned table
 * cascades in the database, so nothing is left behind.
 */
@Service
public class PrivacyService {

    private static final Logger log = LoggerFactory.getLogger(PrivacyService.class);

    private final UserService users;
    private final UserRepository userRepo;
    private final ConsentRecordRepository consents;
    private final CheckInRepository checkIns;
    private final JournalRepository journal;
    private final HabitRepository habits;
    private final HabitPauseRepository pauses;
    private final HabitCompletionRepository completions;
    private final CopingService coping;
    private final Clock clock;

    public PrivacyService(UserService users, UserRepository userRepo, ConsentRecordRepository consents,
                          CheckInRepository checkIns, JournalRepository journal, HabitRepository habits,
                          HabitPauseRepository pauses, HabitCompletionRepository completions,
                          CopingService coping, Clock clock) {
        this.users = users;
        this.userRepo = userRepo;
        this.consents = consents;
        this.checkIns = checkIns;
        this.journal = journal;
        this.habits = habits;
        this.pauses = pauses;
        this.completions = completions;
        this.coping = coping;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ExportDocument export(UUID userId) {
        UserResponse u = users.profile(userId);
        List<ExportConsent> consentRows = consents.findByUserIdOrderByRecordedAtAsc(userId).stream()
                .map(c -> new ExportConsent(c.getConsentType().name(), c.isGranted(), c.getPolicyVersion(), c.getRecordedAt()))
                .toList();
        var checkInRows = checkIns.findByUserIdOrderByLocalDateAsc(userId).stream().map(CheckInService::toResponse).toList();
        var journalRows = journal.findByUserIdOrderByCreatedAtAsc(userId).stream().map(JournalService::toDetail).toList();

        Map<UUID, List<ExportPause>> pausesByHabit = new HashMap<>();
        for (HabitPause p : pauses.findByUserId(userId)) {
            pausesByHabit.computeIfAbsent(p.getHabitId(), k -> new ArrayList<>()).add(new ExportPause(p.getPausedOn(), p.getResumedOn()));
        }
        Map<UUID, List<LocalDate>> doneByHabit = new HashMap<>();
        for (HabitCompletion c : completions.findByUserIdOrderByCompletedOnAsc(userId)) {
            doneByHabit.computeIfAbsent(c.getHabitId(), k -> new ArrayList<>()).add(c.getCompletedOn());
        }
        List<ExportHabit> habitRows = new ArrayList<>();
        for (Habit h : habits.findByUserIdOrderByCreatedAtAsc(userId)) {
            habitRows.add(new ExportHabit(h.getId(), h.getName(), h.getDescription(), h.getCadence(), h.getTargetCount(),
                    h.getStartDate(), h.getEndDate(), pausesByHabit.getOrDefault(h.getId(), List.of()),
                    doneByHabit.getOrDefault(h.getId(), List.of())));
        }
        return new ExportDocument(PrivacyDtos.SCHEMA_VERSION, clock.instant(),
                new ExportProfile(u.id(), u.email(), u.displayName(), u.timezone(), u.theme(), u.createdAt()),
                consentRows, checkInRows, journalRows, habitRows, coping.list(userId));
    }

    /** Requires the current password so a stolen session alone cannot erase the account. */
    @Transactional
    public void deleteAccount(UUID userId, String password) {
        if (!users.passwordMatches(userId, password)) {
            throw new ValidationException("password", "is incorrect");
        }
        if (userRepo.deleteUserById(userId) == 0) {
            throw new NotFoundException("User");
        }
        log.info("Account deleted"); // deliberately no identifiers
    }
}
