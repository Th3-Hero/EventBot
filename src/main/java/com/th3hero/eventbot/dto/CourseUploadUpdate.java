package com.th3hero.eventbot.dto;

import com.th3hero.eventbot.entities.CourseJpa;

public record CourseUploadUpdate(
    String code,
    String name,
    String nickname
) {
    public CourseJpa toJpa() {
        return CourseJpa.builder()
            .code(code)
            .name(name)
            .nickname(nickname)
            .build();
    }
}