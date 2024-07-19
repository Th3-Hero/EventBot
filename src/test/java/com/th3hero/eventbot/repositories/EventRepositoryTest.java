package com.th3hero.eventbot.repositories;

import com.th3hero.eventbot.TestEntities;
import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.entities.EventJpa.EventStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EventRepositoryTest {
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void findAllByCourse() {
        CourseJpa courseOne = TestEntities.courseJpa(1);
        CourseJpa courseTwo = TestEntities.courseJpa(2);
        CourseJpa courseThree = TestEntities.courseJpa(3);
        CourseJpa targetCourse = TestEntities.courseJpa(4);

        courseRepository.saveAllAndFlush(List.of(courseOne, courseTwo, courseThree, targetCourse));

        EventJpa eventOne = TestEntities.eventJpa(1, List.of(courseOne, courseTwo));
        EventJpa eventTwo = TestEntities.eventJpa(2, List.of(courseTwo, courseThree));
        EventJpa eventThree = TestEntities.eventJpa(3, List.of(courseThree, targetCourse));
        eventRepository.saveAllAndFlush(List.of(eventOne, eventTwo, eventThree));
        entityManager.clear();

        List<EventJpa> events = eventRepository.findAllByCourse(List.of(courseOne, courseTwo));

        assertThat(events).containsExactlyInAnyOrder(eventOne, eventTwo);
    }

    @Test
    void findAllByCourse_noEventForCourse() {
        CourseJpa courseOne = TestEntities.courseJpa(1);
        CourseJpa courseTwo = TestEntities.courseJpa(2);
        CourseJpa courseThree = TestEntities.courseJpa(3);
        CourseJpa target = TestEntities.courseJpa(5);

        courseRepository.saveAllAndFlush(List.of(courseOne, courseTwo, courseThree, target));

        EventJpa eventOne = TestEntities.eventJpa(1, List.of(courseOne, courseTwo));
        EventJpa eventTwo = TestEntities.eventJpa(2, List.of(courseTwo, courseThree));
        eventRepository.saveAllAndFlush(List.of(eventOne, eventTwo));
        entityManager.clear();

        List<EventJpa> events = eventRepository.findAllByCourse(List.of(target));

        assertThat(events).isEmpty();
    }

    @Test
    void findAllByCourse_activeOnly() {
        CourseJpa courseOne = TestEntities.courseJpa(1);
        CourseJpa courseTwo = TestEntities.courseJpa(2);
        courseRepository.saveAllAndFlush(List.of(courseOne, courseTwo));

        EventJpa eventOne = TestEntities.eventJpa(1, List.of(courseOne, courseTwo));
        eventOne.setStatus(EventStatus.DELETED);
        EventJpa eventTwo = TestEntities.eventJpa(2, List.of(courseTwo));
        EventJpa eventThree = TestEntities.eventJpa(3, List.of(courseOne));
        eventRepository.saveAllAndFlush(List.of(eventOne, eventTwo, eventThree));
        entityManager.clear();

        List<EventJpa> events = eventRepository.findAllByCourse(List.of(courseOne));

        assertThat(events).containsExactlyInAnyOrder(eventThree);
    }

    @Test
    void findAllActive() {
        EventJpa eventOne = TestEntities.eventJpa(1);
        EventJpa eventTwo = TestEntities.eventJpa(2);
        EventJpa eventThree = TestEntities.eventJpa(3);
        eventRepository.saveAllAndFlush(List.of(eventOne, eventTwo, eventThree));
        entityManager.clear();

        List<EventJpa> events = eventRepository.findAllActive();

        assertThat(events).containsExactlyInAnyOrder(eventOne, eventTwo, eventThree);
    }

    @Test
    void findAllActive_includesNonActive() {
        EventJpa eventOne = TestEntities.eventJpa(1);
        EventJpa eventTwo = TestEntities.eventJpa(2);
        eventTwo.setStatus(EventStatus.DELETED);
        EventJpa eventThree = TestEntities.eventJpa(3);
        eventRepository.saveAllAndFlush(List.of(eventOne, eventTwo, eventThree));
        entityManager.clear();

        List<EventJpa> events = eventRepository.findAllActive();

        assertThat(events).containsExactlyInAnyOrder(eventOne, eventThree);
    }

    @Test
    void findEventJpaByMessageId() {
        Long targetEventId = 1111L;
        EventJpa eventOne = TestEntities.eventJpa(1);
        EventJpa eventTwo = TestEntities.eventJpa(2);
        eventTwo.setMessageId(targetEventId);
        EventJpa eventThree = TestEntities.eventJpa(3);

        eventRepository.saveAllAndFlush(List.of(eventOne, eventTwo, eventThree));
        entityManager.clear();

        Optional<EventJpa> event = eventRepository.findByMessageId(targetEventId);

        assertThat(event).contains(eventTwo);
    }

    @Test
    void findEventJpaByMessageId_noEvent() {
        Long targetEventId = 1111L;
        EventJpa eventOne = TestEntities.eventJpa(1);
        EventJpa eventTwo = TestEntities.eventJpa(2);
        EventJpa eventThree = TestEntities.eventJpa(3);

        eventRepository.saveAllAndFlush(List.of(eventOne, eventTwo, eventThree));
        entityManager.clear();

        Optional<EventJpa> event = eventRepository.findByMessageId(targetEventId);

        assertThat(event).isEmpty();
    }

}