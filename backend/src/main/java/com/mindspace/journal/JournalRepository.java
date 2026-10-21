package com.mindspace.journal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JournalRepository extends JpaRepository<JournalEntry, UUID> {

    String FILTER = """
            j.user_id = :userId
            AND (CAST(:q AS text) IS NULL OR j.search_vector @@ websearch_to_tsquery('simple', CAST(:q AS text)))
            AND (CAST(:from AS date) IS NULL OR j.entry_date >= CAST(:from AS date))
            AND (CAST(:to AS date) IS NULL OR j.entry_date <= CAST(:to AS date))
            AND (CAST(:tag AS text) IS NULL OR EXISTS (
                   SELECT 1 FROM journal_entry_tags t WHERE t.entry_id = j.id AND t.tag = CAST(:tag AS text)))
            """;

    Optional<JournalEntry> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Search/filter, always scoped by user_id, newest first (journal_entries_user_created_idx).
     * All values are bound parameters; websearch_to_tsquery tolerates arbitrary user text.
     * The Pageable must be unsorted: ordering is fixed in the query.
     */
    @Query(value = "SELECT j.* FROM journal_entries j WHERE " + FILTER + " ORDER BY j.created_at DESC, j.id DESC",
            countQuery = "SELECT count(*) FROM journal_entries j WHERE " + FILTER,
            nativeQuery = true)
    Page<JournalEntry> search(@Param("userId") UUID userId, @Param("q") String q, @Param("tag") String tag,
                              @Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);

    List<JournalEntry> findTop5ByUserIdOrderByCreatedAtDescIdDesc(UUID userId);

    long countByUserIdAndEntryDateBetween(UUID userId, LocalDate from, LocalDate to);

    List<JournalEntry> findByUserIdOrderByCreatedAtAsc(UUID userId);

    interface DayCount {
        LocalDate getDate();

        long getCount();
    }

    @Query("select j.entryDate as date, count(j) as count from JournalEntry j "
            + "where j.userId = :userId and j.entryDate between :from and :to group by j.entryDate")
    List<DayCount> countByDay(@Param("userId") UUID userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Modifying
    @Query("delete from JournalEntry j where j.id = :id and j.userId = :userId")
    int deleteByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
