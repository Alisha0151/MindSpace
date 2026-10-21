package com.mindspace.wellness;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CopingStrategyRepository extends JpaRepository<CopingStrategy, UUID> {

    List<CopingStrategy> findByUserIdOrderBySortOrderAscCreatedAtAsc(UUID userId);

    Optional<CopingStrategy> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);

    @Query("select coalesce(max(c.sortOrder), -1) from CopingStrategy c where c.userId = :userId")
    int maxSortOrder(@Param("userId") UUID userId);

    @Modifying
    @Query("delete from CopingStrategy c where c.id = :id and c.userId = :userId")
    int deleteByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
