package com.mindspace.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * Minimal cookie-aware HTTP client for integration tests. Behaves like the SPA: keeps cookies and echoes the
 * XSRF-TOKEN cookie in X-XSRF-TOKEN on writes (unless told not to).
 */
public final class ApiClient {

    public record Reply(int status, JsonNode body, String raw) {
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    private final String base;
    private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private final HttpClient http = HttpClient.newBuilder().cookieHandler(cookies).build();

    public ApiClient(String base) {
        this.base = base;
    }

    public Reply get(String path) {
        return send("GET", path, null, true);
    }

    public Reply post(String path, Object body) {
        return send("POST", path, body, true);
    }

    public Reply put(String path, Object body) {
        return send("PUT", path, body, true);
    }

    public Reply delete(String path) {
        return send("DELETE", path, null, true);
    }

    public Reply deleteWithBody(String path, Object body) {
        return send("DELETE", path, body, true);
    }

    public Reply postWithoutCsrf(String path, Object body) {
        return send("POST", path, body, false);
    }

    public String cookie(String name) {
        return cookies.getCookieStore().getCookies().stream()
                .filter(c -> c.getName().equals(name)).map(HttpCookie::getValue).findFirst().orElse(null);
    }

    /** Replaces a cookie value, e.g. to replay an old refresh token. */
    public void setCookie(String name, String value, String path) {
        // Drop any existing cookie of this name first: otherwise the jar keeps both and the server sees the old one.
        for (HttpCookie existing : new java.util.ArrayList<>(cookies.getCookieStore().getCookies())) {
            if (existing.getName().equals(name)) {
                cookies.getCookieStore().remove(null, existing);
            }
        }
        HttpCookie c = new HttpCookie(name, value);
        c.setPath(path);
        c.setDomain(URI.create(base).getHost());
        cookies.getCookieStore().add(URI.create(base), c);
    }

    public ApiClient bootstrapCsrf() {
        send("GET", "/api/v1/auth/csrf", null, false);
        return this;
    }

    private Reply send(String method, String path, Object body, boolean csrf) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path)).header("Accept", "application/json");
            if (csrf && !method.equals("GET")) {
                String token = cookie("XSRF-TOKEN");
                if (token == null) {
                    bootstrapCsrf();
                    token = cookie("XSRF-TOKEN");
                }
                if (token != null) {
                    b.header("X-XSRF-TOKEN", token);
                }
            }
            if (body != null) {
                b.header("Content-Type", "application/json");
                // Strings are already JSON documents; everything else is serialised.
                String json = body instanceof String str ? str : JSON.writeValueAsString(body);
                b.method(method, HttpRequest.BodyPublishers.ofString(json));
            } else {
                b.method(method, HttpRequest.BodyPublishers.noBody());
            }
            HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode node = r.body() == null || r.body().isBlank() ? null : JSON.readTree(r.body());
            return new Reply(r.statusCode(), node, r.body());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    public static List<String> texts(JsonNode array) {
        return array.findValuesAsText("");
    }
}
