package com.mindspace.auth;

/** Deliberately carries no detail: clients must not learn why authentication failed. */
public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException() {
        super("Authentication failed");
    }
}
