package com.th3hero.eventbot.repositories;

import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.entities.EventJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<EventJpa, Long>, JpaSpecificationExecutor<EventJpa> {

    /**
     * Find all events that have one or more of the given courses. Deleted events are ignored
     *
     * @param courses list of courses
     * @return List of events
     */
    @Query("select e from EventJpa e join e.courses c where c in :courses and e.status = 'ACTIVE'")
    List<EventJpa> findAllByCourse(List<CourseJpa> courses);

    /**
     * Find all events that are active. No deleted, draft, or completed events are returned
     *
     * @return List of active events
     */
    @Query("select e from EventJpa e where e.status = 'ACTIVE'")
    List<EventJpa> findAllActive();

    /**
     * Find an event by its discord message id
     * @param messageId the message id of the event
     * @return {@link Optional} of the event with the given message id
     */
    Optional<EventJpa> findByMessageId(Long messageId);

}
