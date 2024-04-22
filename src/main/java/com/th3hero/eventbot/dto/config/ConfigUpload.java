package com.th3hero.eventbot.dto.config;

import com.th3hero.eventbot.entities.ConfigJpa;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.validation.constraints.Size;
import lombok.NonNull;

public record ConfigUpload(
        @NonNull Long eventChannel,
        @NonNull @Size(min = 4, max = 4, message = "Term must be a 4 character string with year and term. Eg. Y1T2") String term
) {
    public ConfigJpa toJpa() {
        return ConfigJpa.builder()
                .eventChannel(eventChannel)
                .term(term)
                .build();
    }
}
