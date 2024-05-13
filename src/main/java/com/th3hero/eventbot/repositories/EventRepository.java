package com.th3hero.eventbot.repositories;

import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.entities.EventJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<EventJpa, Long> {

    @Query("select e from EventJpa e join e.courses c where c = :course")
    Optional<EventJpa> findByCourse(@Param("course") CourseJpa courseJpa);

    @Query("select e from EventJpa e join e.courses c where c in :courses")
    List<EventJpa> findAllByCourse(@Param("courses")List<CourseJpa> courses);

    boolean existsByMessageId(Long messageId);

    Optional<EventJpa> findEventJpaByMessageId(Long messageId);
}
