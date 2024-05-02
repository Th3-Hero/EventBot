package com.th3hero.eventbot.repositories;

import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.entities.StudentJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRepository extends JpaRepository<StudentJpa, Long> {
    List<StudentJpa> findAllByCoursesContains(CourseJpa courseJpa);
}
