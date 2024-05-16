package com.th3hero.eventbot.repositories;

import com.th3hero.eventbot.TestEntities;
import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.entities.StudentJpa;
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
class StudentRepositoryTest {
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void findAllByCoursesContains() {
        CourseJpa courseOne = TestEntities.courseJpa(1);
        CourseJpa courseTwo = TestEntities.courseJpa(2);
        CourseJpa courseThree = TestEntities.courseJpa(3);
        courseRepository.saveAllAndFlush(List.of(courseOne, courseTwo, courseThree));

        StudentJpa studentOne = TestEntities.studentJpa(1, List.of(courseOne));
        StudentJpa studentTwo = TestEntities.studentJpa(2, List.of(courseOne, courseTwo));
        StudentJpa studentThree = TestEntities.studentJpa(3, List.of(courseTwo));
        StudentJpa studentFour = TestEntities.studentJpa(4, List.of(courseThree));
        StudentJpa studentFive = TestEntities.studentJpa(5, List.of(courseTwo, courseThree));

        studentRepository.saveAllAndFlush(List.of(studentOne, studentTwo, studentThree, studentFour, studentFive));
        entityManager.clear();

        List<StudentJpa> students = studentRepository.findAllByCoursesContains(courseTwo);

        assertThat(students)
            .usingRecursiveComparison()
            .isEqualTo(List.of(studentTwo, studentThree, studentFive));
    }

    @Test
    void findAllByCoursesContains_notStudentsWithCourse() {
        CourseJpa courseOne = TestEntities.courseJpa(1);
        CourseJpa courseTwo = TestEntities.courseJpa(2);
        CourseJpa courseThree = TestEntities.courseJpa(3);
        courseRepository.saveAllAndFlush(List.of(courseOne, courseTwo, courseThree));

        StudentJpa studentOne = TestEntities.studentJpa(1, List.of(courseOne));
        StudentJpa studentTwo = TestEntities.studentJpa(2, List.of(courseOne, courseTwo));

        studentRepository.saveAllAndFlush(List.of(studentOne, studentTwo));
        entityManager.clear();

        List<StudentJpa> students = studentRepository.findAllByCoursesContains(courseThree);

        assertThat(students).isEmpty();
    }
}