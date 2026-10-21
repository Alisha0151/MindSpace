package com.mindspace.privacy;

import com.mindspace.auth.AuthUser;
import com.mindspace.auth.CookieFactory;
import com.mindspace.privacy.PrivacyDtos.DeleteAccountRequest;
import com.mindspace.privacy.PrivacyDtos.ExportDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Privacy", description = "Data export and permanent account deletion.")
public class PrivacyController {

    private final PrivacyService service;
    private final CookieFactory cookies;

    public PrivacyController(PrivacyService service, CookieFactory cookies) {
        this.service = service;
        this.cookies = cookies;
    }

    @Operation(summary = "Download all of the user's data as JSON (schemaVersion 1)")
    @GetMapping("/api/v1/privacy/export")
    public ResponseEntity<ExportDocument> export(@AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mindspace-export.json\"")
                .body(service.export(user.userId()));
    }

    @Operation(summary = "Permanently delete the account and all data (requires the current password)")
    @DeleteMapping("/api/v1/account")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody DeleteAccountRequest request) {
        service.deleteAccount(user.userId(), request.password());
        HttpHeaders headers = new HttpHeaders();
        cookies.addClearingCookies(headers);
        return ResponseEntity.noContent().headers(headers).build();
    }
}
