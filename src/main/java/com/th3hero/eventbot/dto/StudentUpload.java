package com.th3hero.eventbot.dto;

import com.th3hero.eventbot.dto.course.CourseUpload;
import com.th3hero.eventbot.entities.StudentJpa;

import java.util.List;

public record StudentUpload(
        List<CourseUpload> courses
) {
    public StudentJpa toJpa() {
        return StudentJpa.builder()
                .courses(courses.stream().map(CourseUpload::toJpa).toList())
                .build();
    }
}
