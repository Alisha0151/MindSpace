package com.mindspace.wellness;

import com.mindspace.auth.AuthUser;
import com.mindspace.wellness.WellnessDtos.CopingRequest;
import com.mindspace.wellness.WellnessDtos.CopingResponse;
import com.mindspace.wellness.WellnessDtos.ReorderRequest;
import com.mindspace.wellness.WellnessDtos.ToolkitContent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wellness")
@Tag(name = "Wellness toolkit", description = "Breathing, grounding and prompt content plus the user's own coping strategies.")
public class WellnessController {

    private final CopingService coping;

    public WellnessController(CopingService coping) {
        this.coping = coping;
    }

    @Operation(summary = "Breathing patterns, grounding steps and reflection/gratitude prompts (static, non-clinical)")
    @GetMapping("/toolkit")
    public ToolkitContent toolkit() {
        return new ToolkitContent(ToolkitData.BREATHING,
                ToolkitData.GROUNDING,
                ToolkitData.REFLECTION_PROMPTS,
                ToolkitData.GRATITUDE_PROMPTS,
                ToolkitData.DISCLAIMER);
    }

    @GetMapping("/coping-strategies")
    @Operation(summary = "The user's coping strategies in their chosen order")
    public List<CopingResponse> list(@AuthenticationPrincipal AuthUser user) {
        return coping.list(user.userId());
    }

    @PostMapping("/coping-strategies")
    @Operation(summary = "Add a coping strategy (appended to the end)")
    public ResponseEntity<CopingResponse> create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody CopingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(coping.create(user.userId(), request));
    }

    @PutMapping("/coping-strategies/{id}")
    @Operation(summary = "Edit a coping strategy")
    public CopingResponse update(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id, @Valid @RequestBody CopingRequest request) {
        return coping.update(user.userId(), id, request);
    }

    @DeleteMapping("/coping-strategies/{id}")
    @Operation(summary = "Delete a coping strategy")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        coping.delete(user.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/coping-strategies/order")
    @Operation(summary = "Reorder: send every strategy id exactly once in the desired order")
    public List<CopingResponse> reorder(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody ReorderRequest request) {
        return coping.reorder(user.userId(), request.ids());
    }
}
