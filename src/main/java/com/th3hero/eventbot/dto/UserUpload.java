package com.th3hero.eventbot.dto;

import com.th3hero.eventbot.entities.UserJpa;

import java.util.List;

public record UserUpload(
        List<CourseUpload> courses
) {
    public UserJpa toJpa() {
        return UserJpa.builder()
                .courses(courses.stream().map(CourseUpload::toJpa).toList())
                .build();
    }
}
