package com.th3hero.eventbot.dto.course;

import com.th3hero.eventbot.entities.CourseJpa;
import jakarta.validation.constraints.NotNull;

public record CourseUpload(
    @NotNull String code,
    @NotNull String name
) {
    public CourseJpa toJpa() {
        return CourseJpa.builder()
            .code(code)
            .name(name)
            .build();
    }
}
