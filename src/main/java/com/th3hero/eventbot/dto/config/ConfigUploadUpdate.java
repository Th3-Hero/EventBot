package com.th3hero.eventbot.dto.config;

import com.th3hero.eventbot.entities.ConfigJpa;

public record ConfigUploadUpdate(
        Long eventChannel,
        String term
) {
    public ConfigJpa toJpa() {
        return ConfigJpa.builder()
                .eventChannel(eventChannel)
                .term(term)
                .build();
    }
}
