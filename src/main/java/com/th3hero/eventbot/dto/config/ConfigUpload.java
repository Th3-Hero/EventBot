package com.th3hero.eventbot.dto.config;

import com.th3hero.eventbot.entities.ConfigJpa;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.validation.constraints.Size;
import lombok.NonNull;

public record ConfigUpload(
        @NonNull Long eventChannel
) {
    public ConfigJpa toJpa() {
        return ConfigJpa.builder()
                .eventChannel(eventChannel)
                .build();
    }
}
