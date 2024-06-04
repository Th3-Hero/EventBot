package com.th3hero.eventbot.services;

import com.th3hero.eventbot.TestEntities;
import com.th3hero.eventbot.commands.requests.InteractionRequest;
import com.th3hero.eventbot.commands.requests.SelectionRequest;
import com.th3hero.eventbot.dto.course.CourseUpdate;
import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.repositories.CourseRepository;
import com.th3hero.eventbot.repositories.EventRepository;
import jakarta.persistence.EntityNotFoundException;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.requests.restaction.interactions.AutoCompleteCallbackAction;
import net.dv8tion.jda.internal.entities.UserImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private StudentService studentService;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private SchedulingService schedulingService;

    @InjectMocks
    private CourseService courseService;

    @Test
    void createCourses() {
        final var courseUploads = List.of(
            TestEntities.courseUpload(1),
            TestEntities.courseUpload(2),
            TestEntities.courseUpload(3)
        );

        when(courseRepository.saveAll(any()))
            .thenReturn(List.of(
                TestEntities.courseJpa(1),
                TestEntities.courseJpa(2),
                TestEntities.courseJpa(3)
            ));

        final var courses = courseService.createCourses(courseUploads);

        assertThat(courses).hasSize(3).containsExactlyInAnyOrder(
            TestEntities.courseJpa(1).toDto(),
            TestEntities.courseJpa(2).toDto(),
            TestEntities.courseJpa(3).toDto()
        );
    }

    @Test
    @SuppressWarnings("squid:S5778")
    void createCourses_emptyList() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> courseService.createCourses(List.of()));
        verify(courseRepository, never()).saveAll(any());
    }

    @Test
    void updateCourse() {
        final var courseId = 1234L;
        final var courseUpdate = TestEntities.courseUpdate(1);
        final var courseJpa = TestEntities.courseJpa(1);

        when(courseRepository.findById(courseId))
            .thenReturn(Optional.of(courseJpa));
        when(courseRepository.save(any(CourseJpa.class)))
            .thenReturn(courseJpa);

        final var updatedCourse = courseService.updateCourse(courseId, courseUpdate);

        assertThat(updatedCourse).isEqualTo(courseJpa.toDto());
        verify(courseRepository).save(argThat(course ->
            course.getCode().equals(courseUpdate.code()) &&
                course.getName().equals(courseUpdate.name())
        ));
    }

    @Test
    void updateCourse_noCourse() {
        final var courseId = 1234L;
        final var courseUpdate = TestEntities.courseUpdate(1);

        when(courseRepository.findById(courseId))
            .thenReturn(Optional.empty());

        assertThatExceptionOfType(EntityNotFoundException.class)
            .isThrownBy(() -> courseService.updateCourse(courseId, courseUpdate));
        verify(courseRepository, never()).save(any());
    }

    @Test
    void updateCourse_code() {
        final var courseId = 1234L;
        final var courseUpdate = new CourseUpdate("TEST1", null);
        final var courseJpa = TestEntities.courseJpa(1);

        when(courseRepository.findById(courseId))
            .thenReturn(Optional.of(courseJpa));
        when(courseRepository.save(any(CourseJpa.class)))
            .thenReturn(courseJpa);

        final var updatedCourse = courseService.updateCourse(courseId, courseUpdate);

        assertThat(updatedCourse).isEqualTo(courseJpa.toDto());
        verify(courseRepository).save(argThat(course ->
            course.getCode().equals(courseUpdate.code()) &&
                course.getName().equals(courseJpa.getName())
        ));
    }

    @Test
    void updateCourse_name() {
        final var courseId = 1234L;
        final var courseUpdate = new CourseUpdate(null, "Test Course1");
        final var courseJpa = TestEntities.courseJpa(1);

        when(courseRepository.findById(courseId))
            .thenReturn(Optional.of(courseJpa));
        when(courseRepository.save(any(CourseJpa.class)))
            .thenReturn(courseJpa);

        final var updatedCourse = courseService.updateCourse(courseId, courseUpdate);

        assertThat(updatedCourse).isEqualTo(courseJpa.toDto());
        verify(courseRepository).save(argThat(course ->
            course.getCode().equals(courseJpa.getCode()) &&
                course.getName().equals(courseUpdate.name())
        ));
    }

    @Test
    void deleteCourseById() {
        final var courseId = 1234L;
        final var courseJpa = TestEntities.courseJpa(1);

        when(courseRepository.findById(courseId))
            .thenReturn(Optional.of(courseJpa));

        courseService.deleteCourseById(courseId);

        verify(studentService).removeCourseFromAllStudents(courseJpa);
        verify(courseRepository).deleteById(courseId);
    }

    @Test
    void deleteCourseById_noCourse() {
        final var courseId = 1234L;

        when(courseRepository.findById(courseId))
            .thenReturn(Optional.empty());

        assertThatExceptionOfType(EntityNotFoundException.class)
            .isThrownBy(() -> courseService.deleteCourseById(courseId));
        verify(studentService, never()).removeCourseFromAllStudents(any());
        verify(courseRepository, never()).deleteById(any());
    }

    @Test
    void createCourseSelectionMenu() {
        final var menuId = "MENU";
        final var courseOne = TestEntities.courseJpa(1);
        final var courseTwo = TestEntities.courseJpa(2);
        final var courseThree = TestEntities.courseJpa(3);
        final var courses = List.of(courseOne, courseTwo, courseThree);

        when(courseRepository.findAll())
            .thenReturn(courses);

        final var menu = courseService.createCourseSelectionMenu(menuId, List.of(courseTwo));

        assertThat(menu.getId()).isEqualTo(menuId);
        assertThat(menu.getOptions().stream().map(SelectOption::getLabel).toList()).containsExactlyInAnyOrder(
            courseOne.getCode(),
            courseTwo.getCode(),
            courseThree.getCode()
        );
        assertThat(menu.getOptions().stream().map(SelectOption::getDescription).toList()).containsExactlyInAnyOrder(
            courseOne.getName(),
            courseTwo.getName(),
            courseThree.getName()
        );
    }

    @Test
    void coursesFromCourseCodes() {
        final var courseOne = TestEntities.courseJpa(1);
        final var courseTwo = TestEntities.courseJpa(2);
        final var courseThree = TestEntities.courseJpa(3);
        final var courses = List.of(courseOne, courseTwo, courseThree);

        when(courseRepository.findByCodeIn(any()))
            .thenReturn(courses);

        final var courseCodes = courses.stream().map(CourseJpa::getCode).toList();
        final var result = courseService.coursesFromCourseCodes(courseCodes);

        assertThat(result).containsExactlyInAnyOrderElementsOf(courses);
    }

    @Test
    void coursesFromCourseCodes_failedToFindAllCourses() {
        final var courseOne = TestEntities.courseJpa(1);
        final var courseTwo = TestEntities.courseJpa(2);
        final var courseThree = TestEntities.courseJpa(3);
        final var courses = List.of(courseOne, courseTwo, courseThree);

        when(courseRepository.findByCodeIn(any()))
            .thenReturn(List.of(courseOne, courseThree));

        final var courseCodes = courses.stream().map(CourseJpa::getCode).toList();

        assertThatExceptionOfType(EntityNotFoundException.class)
            .isThrownBy(() -> courseService.coursesFromCourseCodes(courseCodes));
    }

    @Test
    void processStudentSelectedCourses() {
        final var studentId = 1L;
        final var request = mock(SelectionRequest.class);
        final var event = mock(StringSelectInteractionEvent.class);
        final var courseOne = TestEntities.courseJpa(1);
        final var courseTwo = TestEntities.courseJpa(2);
        final var courseThree = TestEntities.courseJpa(3);
        final var student = TestEntities.studentJpa(1, List.of(courseOne, courseTwo, courseThree));
        final var updatedCourses = List.of(courseOne, courseTwo);
        final var removedCourses = List.of(courseThree);
        final var eventsToRemove = List.of(TestEntities.eventJpaWithId(1));
        final var eventsToAdd = List.of(TestEntities.eventJpa(2));

        final var member = TestEntities.member();

        when(request.getRequester())
            .thenReturn(member);
        when(request.getEvent())
            .thenReturn(event);
        when(event.getValues())
            .thenReturn(updatedCourses.stream().map(CourseJpa::getCode).collect(Collectors.toList()));
        when(studentService.fetchStudent(studentId))
            .thenReturn(student);
        when(eventRepository.findAllByCourse(removedCourses))
            .thenReturn(eventsToRemove);
        when(eventRepository.findAllByCourse(updatedCourses))
            .thenReturn(eventsToAdd);
        when(courseRepository.findByCodeIn(any()))
            .thenReturn(updatedCourses);

        courseService.processStudentSelectedCourses(request);

        verify(schedulingService).removeEventReminderTriggers(anyLong(), anyLong());
        verify(studentService).scheduleStudentForEvent(any(), eq(student));
        verify(request).sendResponse(any(), eq(InteractionRequest.MessageMode.USER));
    }

    @Test
    void autoCompleteCourseOptions() {
        var event = mock(CommandAutoCompleteInteractionEvent.class);
        var studentCourses = List.of(TestEntities.courseJpa(1));
        var student = TestEntities.studentJpa(1, studentCourses);
        var focusedOption = mock(AutoCompleteQuery.class);
        var autoCompleteCallbackAction = mock(AutoCompleteCallbackAction.class);

        when(event.getUser())
            .thenReturn(new UserImpl(1L, null));
        when(studentService.fetchStudent(anyLong()))
            .thenReturn(student);
        when(event.getFocusedOption())
            .thenReturn(focusedOption);
        when(focusedOption.getValue())
            .thenReturn("TEST");
        when(event.replyChoices(anyList()))
            .thenReturn(autoCompleteCallbackAction);

        courseService.autoCompleteCourseOptions(event);

        verify(event).replyChoices(anyList());
        verify(autoCompleteCallbackAction).queue();
    }

    @Test
    void autoCompleteCourseOptions_noMatchingCourses() {
        var event = mock(CommandAutoCompleteInteractionEvent.class);
        var studentCourses = List.of(TestEntities.courseJpa(1));
        var student = TestEntities.studentJpa(1, studentCourses);
        var focusedOption = mock(AutoCompleteQuery.class);
        var autoCompleteCallbackAction = mock(AutoCompleteCallbackAction.class);

        when(event.getUser())
            .thenReturn(new UserImpl(1L, null));
        when(studentService.fetchStudent(anyLong()))
            .thenReturn(student);
        when(event.getFocusedOption())
            .thenReturn(focusedOption);
        when(focusedOption.getValue())
            .thenReturn("NON_EXISTING_COURSE");
        when(event.replyChoices(anyList()))
            .thenReturn(autoCompleteCallbackAction);

        courseService.autoCompleteCourseOptions(event);

        verify(event).replyChoices(List.of());
    }
}