package com.th3hero.eventbot.dto;

import com.th3hero.eventbot.dto.course.Course;
import lombok.NonNull;

import java.util.List;

public record Student(
        @NonNull Long id,
        List<Course> courses
) {}
