package com.mindspace.common;

/** Also used when a resource exists but belongs to another user, so ids cannot be probed. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String resource) {
        super(resource + " not found");
    }
}
