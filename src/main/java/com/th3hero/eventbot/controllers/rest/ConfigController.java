package com.th3hero.eventbot.controllers.rest;

import com.th3hero.eventbot.dto.config.Config;
import com.th3hero.eventbot.dto.config.ConfigUpdate;
import com.th3hero.eventbot.dto.config.ConfigUpload;
import com.th3hero.eventbot.services.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/config")
@Tag(name = "Config Controller", description = "Handles admin operations regarding dynamic configuration")
public class ConfigController {
    private final ConfigService configService;

    @GetMapping
    @Operation(summary = "Returns an existing config")
    public Config getConfig() {
        return configService.getConfig();
    }

    @PostMapping
    @Operation(summary = "Create a new config if non exists")
    @ResponseStatus(HttpStatus.CREATED)
    public Config createConfig(
        @RequestBody @NotNull @Valid ConfigUpload configUpload
    ) {
        return configService.createConfig(configUpload);
    }

    @PatchMapping
    @Operation(summary = "Update the existing config")
    public Config updateConfig(
        @RequestBody @NotNull @Valid ConfigUpdate configUploadUpdate
    ) {
        return configService.updateConfig(configUploadUpdate);
    }
}
