package com.th3hero.eventbot.repositories;

import com.th3hero.eventbot.entities.CourseJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface CourseRepository extends JpaRepository<CourseJpa, Long> {
    Optional<CourseJpa> findCourseJpaByCode(String courseCode);
}
