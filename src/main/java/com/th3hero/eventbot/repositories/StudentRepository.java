package com.th3hero.eventbot.repositories;

import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.entities.StudentJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<StudentJpa, Long> {
    /**
     * Find all students that are in the given course
     * @param courseJpa the course to find students for
     * @return List of students that are in the given course
     */
    List<StudentJpa> findAllByCoursesContains(CourseJpa courseJpa);

}
