package com.mindspace.auth;

import java.util.UUID;

/** The authenticated principal placed in the SecurityContext. */
public record AuthUser(UUID userId, UUID sessionFamilyId) {
}
