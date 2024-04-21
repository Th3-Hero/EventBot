package com.th3hero.eventbot.dto;

import lombok.NonNull;
import com.th3hero.eventbot.entities.CourseJpa;

public record CourseUpload(
    @NonNull String code,
    @NonNull String name,
    @NonNull String nickname
) {
    public CourseJpa toJpa() {
        return CourseJpa.builder()
                .code(code)
                .name(name)
                .nickname(nickname)
                .build();
    }
}
