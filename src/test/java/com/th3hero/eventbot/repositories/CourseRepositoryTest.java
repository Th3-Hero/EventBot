package com.th3hero.eventbot.repositories;

import com.th3hero.eventbot.TestEntities;
import com.th3hero.eventbot.entities.CourseJpa;
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
class CourseRepositoryTest {
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void findCourseJpaByCode() {
        final CourseJpa courseOne = TestEntities.courseJpa(1);
        final CourseJpa courseTwo = TestEntities.courseJpa(2);
        final CourseJpa courseThree = TestEntities.courseJpa(3);

        courseRepository.saveAllAndFlush(List.of(courseOne, courseTwo, courseThree));
        entityManager.clear();

        final Optional<CourseJpa> course = courseRepository.findByCode(courseTwo.getCode());

        assertThat(course).contains(courseTwo);
    }

    @Test
    void findCourseJpaByCode_missingCourse() {
        final CourseJpa courseOne = TestEntities.courseJpa(1);
        final CourseJpa courseTwo = TestEntities.courseJpa(2);
        final CourseJpa courseThree = TestEntities.courseJpa(4);

        courseRepository.saveAllAndFlush(List.of(courseOne, courseTwo));
        entityManager.clear();

        final Optional<CourseJpa> course = courseRepository.findByCode(courseThree.getCode());

        assertThat(course).isEmpty();
    }
}