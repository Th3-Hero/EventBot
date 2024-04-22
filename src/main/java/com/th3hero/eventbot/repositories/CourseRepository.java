package com.th3hero.eventbot.repositories;

import com.th3hero.eventbot.entities.CourseJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface CourseRepository extends JpaRepository<CourseJpa, Integer> {
    Optional<CourseJpa> findCourseJpaByCode(String courseCode);
}
