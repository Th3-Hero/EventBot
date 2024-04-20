package com.th3hero.eventbot.repositories;

import com.th3hero.eventbot.entities.CourseJpa;
import org.springframework.data.jpa.repository.JpaRepository;


public interface CourseRepository extends JpaRepository<CourseJpa, Integer> {

}
