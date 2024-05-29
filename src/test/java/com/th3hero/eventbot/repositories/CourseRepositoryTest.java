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
    void findByCodeIn() {
        CourseJpa courseOne = TestEntities.courseJpa(1);
        CourseJpa courseTwo = TestEntities.courseJpa(2);
        CourseJpa courseThree = TestEntities.courseJpa(3);

        courseRepository.saveAllAndFlush(List.of(courseOne, courseTwo, courseThree));
        entityManager.clear();

        List<CourseJpa> courses = courseRepository.findByCodeIn(List.of(courseOne.getCode(), courseThree.getCode()));

        assertThat(courses).containsExactlyInAnyOrder(courseOne, courseThree);
    }

    @Test
    void findByCodeIn_noMatchingCourse() {
        CourseJpa courseOne = TestEntities.courseJpa(1);
        CourseJpa courseTwo = TestEntities.courseJpa(2);
        CourseJpa courseThree = TestEntities.courseJpa(3);
        CourseJpa courseFour = TestEntities.courseJpa(4);

        courseRepository.saveAllAndFlush(List.of(courseOne, courseTwo, courseThree));
        entityManager.clear();

        List<CourseJpa> courses = courseRepository.findByCodeIn(List.of(courseFour.getCode()));

        assertThat(courses).isEmpty();
    }
}