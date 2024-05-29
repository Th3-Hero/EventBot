package com.th3hero.eventbot.services;

import com.th3hero.eventbot.TestEntities;
import com.th3hero.eventbot.commands.requests.ButtonRequest;
import com.th3hero.eventbot.commands.requests.CommandRequest;
import com.th3hero.eventbot.commands.requests.InteractionRequest;
import com.th3hero.eventbot.entities.StudentJpa;
import com.th3hero.eventbot.repositories.EventRepository;
import com.th3hero.eventbot.repositories.StudentRepository;
import com.th3hero.eventbot.utils.DiscordFieldsUtils;
import jakarta.persistence.EntityNotFoundException;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.requests.restaction.interactions.AutoCompleteCallbackAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.th3hero.eventbot.utils.DiscordFieldsUtils.OFFSET;
import static com.th3hero.eventbot.utils.DiscordFieldsUtils.SUB_COMMAND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private SchedulingService schedulingService;
    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private StudentService studentService;

    @Test
    void fetchStudent() {
        final var studentId = 1L;
        final var studentJpa = TestEntities.studentJpa(1, List.of());

        when(studentRepository.findById(studentId))
            .thenReturn(Optional.of(studentJpa));

        final var result = studentService.fetchStudent(studentId);

        assertThat(result).isEqualTo(studentJpa);
    }

    @Test
    void fetchStudent_notFound() {
        final var studentId = 1L;

        final var student = StudentJpa.create(studentId);

        when(studentRepository.findById(studentId))
            .thenReturn(Optional.empty());
        when(studentRepository.save(any(StudentJpa.class)))
            .thenAnswer(i -> i.getArgument(0));

        final var result = studentService.fetchStudent(studentId);

        assertThat(result).isEqualTo(student);
    }

    @Test
    void listStudentCourses() {
        final var courses = List.of(TestEntities.courseJpa(1), TestEntities.courseJpa(2), TestEntities.courseJpa(3));
        final var studentJpa = TestEntities.studentJpa(1, courses);
        final var interactionRequest = mock(InteractionRequest.class);
        final var member = mock(Member.class);

        when(interactionRequest.getRequester())
            .thenReturn(member);
        when(interactionRequest.getRequester().getIdLong())
            .thenReturn(studentJpa.getId());
        when(studentRepository.findById(studentJpa.getId()))
            .thenReturn(Optional.of(studentJpa));

        studentService.listStudentCourses(interactionRequest);

        verify(interactionRequest).sendResponse(any(MessageEmbed.class), eq(InteractionRequest.MessageMode.USER));
    }

    @Test
    void listStudentCourses_noCourses() {
        final var studentJpa = TestEntities.studentJpa(1, List.of());
        final var interactionRequest = mock(InteractionRequest.class);
        final var member = mock(Member.class);

        when(interactionRequest.getRequester())
            .thenReturn(member);
        when(interactionRequest.getRequester().getIdLong())
            .thenReturn(studentJpa.getId());
        when(studentRepository.findById(studentJpa.getId()))
            .thenReturn(Optional.of(studentJpa));

        studentService.listStudentCourses(interactionRequest);

        verify(interactionRequest).sendResponse(anyString(), eq(InteractionRequest.MessageMode.USER));
    }

    @Test
    void listStudentCourses_studentNotFound() {
        final var studentId = 1L;
        final var interactionRequest = mock(InteractionRequest.class);
        final var member = mock(Member.class);

        when(interactionRequest.getRequester())
            .thenReturn(member);
        when(interactionRequest.getRequester().getIdLong())
            .thenReturn(studentId);
        when(studentRepository.findById(studentId))
            .thenReturn(Optional.empty());

        assertThatExceptionOfType(EntityNotFoundException.class)
            .isThrownBy(() -> studentService.listStudentCourses(interactionRequest));
    }

    @Test
    void reminderOffsetAutoComplete() {
        final var studentId = 1L;
        final var courses = List.of(TestEntities.courseJpa(1), TestEntities.courseJpa(2), TestEntities.courseJpa(3));
        final var studentJpa = TestEntities.studentJpa(1, courses);

        when(studentRepository.findById(studentId))
            .thenReturn(Optional.of(studentJpa));

        var event = mock(CommandAutoCompleteInteractionEvent.class);
        var user = mock(User.class);
        var option = mock(AutoCompleteQuery.class);
        var autoCompleteCallbackAction = mock(AutoCompleteCallbackAction.class);

        when(event.getUser())
            .thenReturn(user);
        when(user.getIdLong())
            .thenReturn(studentId);
        when(event.getFocusedOption())
            .thenReturn(option);
        when(option.getValue())
            .thenReturn("1");
        when(event.replyChoices(anyList()))
            .thenReturn(autoCompleteCallbackAction);

        studentService.reminderOffsetAutoComplete(event);

        verify(event).replyChoices(anyList());
    }

    @Test
    void reminderOffsetAutoComplete_noMatching() {
        final var studentId = 1L;
        final var courses = List.of(TestEntities.courseJpa(1), TestEntities.courseJpa(2), TestEntities.courseJpa(3));
        final var studentJpa = TestEntities.studentJpa(1, courses);

        when(studentRepository.findById(studentId))
            .thenReturn(Optional.of(studentJpa));

        var event = mock(CommandAutoCompleteInteractionEvent.class);
        var user = mock(User.class);
        var option = mock(AutoCompleteQuery.class);
        var autoCompleteCallbackAction = mock(AutoCompleteCallbackAction.class);

        when(event.getUser())
            .thenReturn(user);
        when(user.getIdLong())
            .thenReturn(studentId);
        when(event.getFocusedOption())
            .thenReturn(option);
        when(option.getValue())
            .thenReturn("4");
        when(event.replyChoices(anyList()))
            .thenReturn(autoCompleteCallbackAction);

        studentService.reminderOffsetAutoComplete(event);

        verify(event).replyChoices(List.of());
    }

    @Test
    void reminderOffsetSubcommandHandler_list() {
        final Map<String, String> arguments = new HashMap<>();
        arguments.put(SUB_COMMAND, StudentService.ReminderConfigOptions.LIST.name());
        final var request = mock(CommandRequest.class);
        final var member = mock(Member.class);
        final var student = TestEntities.studentJpa(1, List.of());


        when(request.getArguments())
            .thenReturn(arguments);
        when(request.getRequester())
            .thenReturn(member);
        when(request.getRequester().getIdLong())
            .thenReturn(student.getId());
        when(studentRepository.findById(student.getId()))
            .thenReturn(Optional.of(student));

        studentService.reminderOffsetSubcommandHandler(request);

        verify(request).sendResponse(any(MessageEmbed.class), eq(InteractionRequest.MessageMode.USER));
    }

    @Test
    void reminderOffsetSubcommandHandler_add() {
        final Map<String, String> arguments = new HashMap<>();
        arguments.put(SUB_COMMAND, StudentService.ReminderConfigOptions.ADD.name());
        arguments.put(OFFSET, "48");
        final var request = mock(CommandRequest.class);
        final var member = mock(Member.class);
        final var events = List.of(TestEntities.eventJpa(1), TestEntities.eventJpa(2));
        final var courses = List.of(TestEntities.courseJpa(1), TestEntities.courseJpa(2));
        final var student = TestEntities.studentJpa(1, courses);


        when(request.getArguments())
            .thenReturn(arguments);
        when(request.getRequester())
            .thenReturn(member);
        when(request.getRequester().getIdLong())
            .thenReturn(student.getId());
        when(studentRepository.findById(student.getId()))
            .thenReturn(Optional.of(student));
        when(eventRepository.findAllByCourse(student.getCourses()))
            .thenReturn(events);

        studentService.reminderOffsetSubcommandHandler(request);

        verify(request).sendResponse(anyString(), eq(InteractionRequest.MessageMode.USER));
        verify(request, never()).sendResponse("You already have an offset for 48", InteractionRequest.MessageMode.USER);
        verify(schedulingService, times(events.size())).addEventReminderTrigger(any(), any(), any(), any());
    }

    @Test
    void reminderOffsetSubcommandHandler_add_existing() {
        final Map<String, String> arguments = new HashMap<>();
        arguments.put(SUB_COMMAND, StudentService.ReminderConfigOptions.ADD.name());
        arguments.put(OFFSET, "48");
        final var request = mock(CommandRequest.class);
        final var member = mock(Member.class);
        final var student = TestEntities.studentJpa(1, List.of());
        student.getReminderOffsetTimes().add(48);


        when(request.getArguments())
            .thenReturn(arguments);
        when(request.getRequester())
            .thenReturn(member);
        when(request.getRequester().getIdLong())
            .thenReturn(student.getId());
        when(studentRepository.findById(student.getId()))
            .thenReturn(Optional.of(student));

        studentService.reminderOffsetSubcommandHandler(request);

        verify(request).sendResponse("You already have an offset for 48", InteractionRequest.MessageMode.USER);
        verify(eventRepository, never()).findAllByCourse(any());
    }

    @Test
    void reminderOffsetSubcommandHandler_remove() {
        final Map<String, String> arguments = new HashMap<>();
        arguments.put(SUB_COMMAND, StudentService.ReminderConfigOptions.REMOVE.name());
        arguments.put(OFFSET, "48");
        final var request = mock(CommandRequest.class);
        final var member = mock(Member.class);
        final var student = TestEntities.studentJpa(1, List.of());
        final var events = List.of(TestEntities.eventJpa(1), TestEntities.eventJpa(2));
        student.getReminderOffsetTimes().add(48);


        when(request.getArguments())
            .thenReturn(arguments);
        when(request.getRequester())
            .thenReturn(member);
        when(request.getRequester().getIdLong())
            .thenReturn(student.getId());
        when(studentRepository.findById(student.getId()))
            .thenReturn(Optional.of(student));
        when(eventRepository.findAllByCourse(student.getCourses()))
            .thenReturn(events);

        studentService.reminderOffsetSubcommandHandler(request);

        verify(request).sendResponse(anyString(), eq(InteractionRequest.MessageMode.USER));
        verify(request, never()).sendResponse("You have no offset for 48", InteractionRequest.MessageMode.USER);
        verify(eventRepository).findAllByCourse(any());
        verify(schedulingService, times(2)).removeEventReminderTriggers(any(), any(), any());
    }

    @Test
    void reminderOffsetSubcommandHandler_remove_missingOffset() {
        final Map<String, String> arguments = new HashMap<>();
        arguments.put(SUB_COMMAND, StudentService.ReminderConfigOptions.REMOVE.name());
        arguments.put(OFFSET, "48");
        final var request = mock(CommandRequest.class);
        final var member = mock(Member.class);
        final var student = TestEntities.studentJpa(1, List.of());

        when(request.getArguments())
            .thenReturn(arguments);
        when(request.getRequester())
            .thenReturn(member);
        when(request.getRequester().getIdLong())
            .thenReturn(student.getId());
        when(studentRepository.findById(student.getId()))
            .thenReturn(Optional.of(student));

        studentService.reminderOffsetSubcommandHandler(request);

        verify(request).sendResponse("You have no offset for 48", InteractionRequest.MessageMode.USER);
        verify(eventRepository, never()).findAllByCourse(any());
    }

    @Test
    void scheduleStudentForEvent() {
        final var studentJpa = TestEntities.studentJpa(1, List.of());
        final var eventJpa = TestEntities.eventJpa(1);

        studentService.scheduleStudentForEvent(eventJpa, studentJpa);

        verify(schedulingService, times(studentJpa.getReminderOffsetTimes().size())).addEventReminderTrigger(
            eq(eventJpa.getId()),
            eq(studentJpa.getId()),
            argThat(i -> studentJpa.getReminderOffsetTimes().contains(i)),
            any(LocalDateTime.class)
        );
    }

    @Test
    void scheduleStudentForEvent_reminderHasPassed() {
        final var studentJpa = TestEntities.studentJpa(1, List.of());
        final var eventJpa = TestEntities.eventJpa(1);
        eventJpa.setEventDate(LocalDateTime.now().minusHours(1));

        studentService.scheduleStudentForEvent(eventJpa, studentJpa);

        verify(schedulingService, never()).addEventReminderTrigger(any(), any(), any(), any());
    }

    @Test
    void unscheduleStudentRemindersForEvent() {
        final var eventId = 1L;
        final var buttonRequest = mock(ButtonRequest.class);
        final var member = mock(Member.class);
        final var studentJpa = TestEntities.studentJpa(1, List.of());

        when(buttonRequest.getRequester())
            .thenReturn(member);
        when(member.getIdLong())
            .thenReturn(studentJpa.getId());
        when(studentRepository.findById(studentJpa.getId()))
            .thenReturn(Optional.of(studentJpa));
        when(schedulingService.removeEventReminderTriggers(eventId, studentJpa.getId()))
            .thenReturn(true);

        studentService.unscheduleStudentRemindersForEvent(buttonRequest, eventId);

        verify(schedulingService).removeEventReminderTriggers(eventId, studentJpa.getId());
        verify(buttonRequest).sendResponse("All reminders have been removed for this event.", InteractionRequest.MessageMode.USER);
    }

    @Test
    void unscheduleStudentRemindersForEvent_noReminders() {
        final var eventId = 1L;
        final var buttonRequest = mock(ButtonRequest.class);
        final var member = mock(Member.class);
        final var studentJpa = TestEntities.studentJpa(1, List.of());

        when(buttonRequest.getRequester())
            .thenReturn(member);
        when(member.getIdLong())
            .thenReturn(studentJpa.getId());
        when(studentRepository.findById(studentJpa.getId()))
            .thenReturn(Optional.of(studentJpa));
        when(schedulingService.removeEventReminderTriggers(eventId, studentJpa.getId()))
            .thenReturn(false);

        studentService.unscheduleStudentRemindersForEvent(buttonRequest, eventId);

        verify(schedulingService).removeEventReminderTriggers(eventId, studentJpa.getId());
        verify(buttonRequest).sendResponse("You have no reminders on this event.", InteractionRequest.MessageMode.USER);
    }

    @Test
    void removeCourseFromAllStudents() {
        final var course = TestEntities.courseJpa(1);
        final var studentOne = TestEntities.studentJpa(1, List.of(course));
        final var studentTwo = TestEntities.studentJpa(2, List.of(course));

        when(studentRepository.findAllByCoursesContains(course))
            .thenReturn(List.of(studentOne, studentTwo));

        studentService.removeCourseFromAllStudents(course);

        assertThat(studentOne.getCourses()).doesNotContain(course);
        assertThat(studentTwo.getCourses()).doesNotContain(course);
    }

    @Test
    void removeCourseFromAllStudents_noStudentsOnCourse() {
        final var course = TestEntities.courseJpa(1);

        when(studentRepository.findAllByCoursesContains(course))
            .thenReturn(List.of());

        studentService.removeCourseFromAllStudents(course);

        verify(studentRepository).findAllByCoursesContains(course);
    }

}