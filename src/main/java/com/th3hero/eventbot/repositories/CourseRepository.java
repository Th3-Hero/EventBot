package com.th3hero.eventbot.repositories;

import com.th3hero.eventbot.entities.CourseJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<CourseJpa, Long> {

    /**
     * Find a list of courses by their course codes
     * @param courseCodes List of course codes
     * @return List of courses with the given course codes
     */
    List<CourseJpa> findByCodeIn(List<String> courseCodes);

}
