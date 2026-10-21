package com.mindspace.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** JSON problem responses for the two failures that happen inside the security filter chain. */
@Component
public class ApiSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, "unauthenticated", "Authentication required",
                "Sign in to access this resource.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) throws IOException {
        // Almost always a missing/invalid CSRF token on a state-changing request.
        write(response, HttpStatus.FORBIDDEN, "forbidden", "Forbidden",
                "The request was rejected (missing or invalid CSRF token).");
    }

    private static void write(HttpServletResponse response, HttpStatus status, String code, String title, String detail)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        String requestId = MDC.get("requestId");
        response.getWriter().write("{\"type\":\"urn:mindspace:problem:" + code + "\",\"title\":\"" + title
                + "\",\"status\":" + status.value() + ",\"detail\":\"" + detail + "\""
                + (requestId != null ? ",\"requestId\":\"" + requestId + "\"" : "") + "}");
    }
}
