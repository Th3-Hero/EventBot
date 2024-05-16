package com.th3hero.eventbot.repositories;

import com.th3hero.eventbot.entities.CourseJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<CourseJpa, Long> {
    Optional<CourseJpa> findCourseJpaByCode(String courseCode);
}
