package com.mindspace.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Bulk delete: the database cascades to every owned table (see docs/DATABASE.md). */
    @Modifying
    @Query("delete from User u where u.id = :id")
    int deleteUserById(@Param("id") UUID id);
}
