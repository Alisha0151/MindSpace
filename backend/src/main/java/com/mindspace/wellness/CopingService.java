package com.mindspace.wellness;

import com.mindspace.common.ConflictException;
import com.mindspace.common.FieldViolation;
import com.mindspace.common.NotFoundException;
import com.mindspace.common.Text;
import com.mindspace.common.ValidationException;
import com.mindspace.wellness.WellnessDtos.CopingRequest;
import com.mindspace.wellness.WellnessDtos.CopingResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CopingService {

    static final int MAX_STRATEGIES = 100;

    private final CopingStrategyRepository repo;

    public CopingService(CopingStrategyRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<CopingResponse> list(UUID userId) {
        return repo.findByUserIdOrderBySortOrderAscCreatedAtAsc(userId).stream().map(CopingService::toResponse).toList();
    }

    @Transactional
    public CopingResponse create(UUID userId, CopingRequest req) {
        validate(req);
        if (repo.countByUserId(userId) >= MAX_STRATEGIES) {
            throw new ConflictException("You can keep up to " + MAX_STRATEGIES + " coping strategies");
        }
        CopingStrategy saved = repo.saveAndFlush(new CopingStrategy(userId, Text.trim(req.title()),
                Text.isBlank(req.details()) ? null : req.details().strip(), repo.maxSortOrder(userId) + 1));
        return toResponse(saved);
    }

    @Transactional
    public CopingResponse update(UUID userId, UUID id, CopingRequest req) {
        validate(req);
        CopingStrategy s = repo.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Coping strategy"));
        s.setTitle(Text.trim(req.title()));
        s.setDetails(Text.isBlank(req.details()) ? null : req.details().strip());
        return toResponse(repo.saveAndFlush(s));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        if (repo.deleteByIdAndUserId(id, userId) == 0) {
            throw new NotFoundException("Coping strategy");
        }
    }

    /** The submitted list must be a permutation of the user's own ids; unknown (or other users') ids are rejected. */
    @Transactional
    public List<CopingResponse> reorder(UUID userId, List<UUID> ids) {
        List<CopingStrategy> mine = repo.findByUserIdOrderBySortOrderAscCreatedAtAsc(userId);
        Set<UUID> submitted = new HashSet<>(ids);
        Set<UUID> owned = mine.stream().map(CopingStrategy::getId).collect(Collectors.toSet());
        if (submitted.size() != ids.size() || !submitted.equals(owned)) {
            throw new ValidationException(List.of(new FieldViolation("ids",
                    "must list each of your strategies exactly once")));
        }
        Map<UUID, CopingStrategy> byId = mine.stream().collect(Collectors.toMap(CopingStrategy::getId, Function.identity()));
        for (int i = 0; i < ids.size(); i++) {
            byId.get(ids.get(i)).setSortOrder(i);
        }
        repo.saveAll(byId.values());
        repo.flush();
        return list(userId);
    }

    private static void validate(CopingRequest req) {
        if (Text.isBlank(req.title())) {
            throw new ValidationException(List.of(new FieldViolation("title", "must not be blank")));
        }
    }

    static CopingResponse toResponse(CopingStrategy s) {
        return new CopingResponse(s.getId(), s.getTitle(), s.getDetails(), s.getSortOrder(), s.getCreatedAt());
    }
}
