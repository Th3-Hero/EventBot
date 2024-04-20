package com.th3hero.eventbot.dto;

import lombok.NonNull;

import java.util.List;

public record User(
        @NonNull Long id,
        List<Course> courses
) {}
