package com.th3hero.eventbot.repositories;

import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.entities.EventJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<EventJpa, Long> {

    @Query("select e from EventJpa e join e.courses c where c in :courses")
    List<EventJpa> findAllByCourse(List<CourseJpa> courses);

    boolean existsByMessageId(Long messageId);

    Optional<EventJpa> findByMessageId(Long messageId);
}
