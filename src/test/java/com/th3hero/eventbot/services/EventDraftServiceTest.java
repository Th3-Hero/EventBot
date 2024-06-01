package com.th3hero.eventbot.services;

import com.th3hero.eventbot.TestEntities;
import com.th3hero.eventbot.commands.actions.ButtonAction;
import com.th3hero.eventbot.commands.actions.SelectionAction;
import com.th3hero.eventbot.commands.requests.ButtonRequest;
import com.th3hero.eventbot.commands.requests.CommandRequest;
import com.th3hero.eventbot.commands.requests.InteractionRequest.MessageMode;
import com.th3hero.eventbot.commands.requests.ModalRequest;
import com.th3hero.eventbot.commands.requests.SelectionRequest;
import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.entities.EventDraftJpa;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.exceptions.DataAccessException;
import com.th3hero.eventbot.exceptions.IllegalInteractionException;
import com.th3hero.eventbot.formatting.DateFormatter;
import com.th3hero.eventbot.repositories.CourseRepository;
import com.th3hero.eventbot.repositories.EventDraftRepository;
import com.th3hero.eventbot.utils.DiscordFieldsUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.th3hero.eventbot.formatting.InteractionArguments.DRAFT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventDraftServiceTest {
    @Mock
    private EventDraftRepository eventDraftRepository;
    @Mock
    private CourseService courseService;
    @Mock
    private SchedulingService schedulingService;
    @Mock
    private EventService eventService;
    @Mock
    private ConfigService configService;
    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private EventDraftService eventDraftService;

    @Test
    void handleEventDraftActions_editDraftDetails() {
        final var request = mock(ButtonRequest.class);
        final Map<String, Long> arguments = Map.of(DRAFT_ID, 1L);
        final var draft = TestEntities.eventDraftJpa(1);

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventDraftRepository.findById(1L))
            .thenReturn(Optional.of(draft));
        when(request.getAction())
            .thenReturn(ButtonAction.EDIT_DRAFT_DETAILS);

        eventDraftService.handleEventDraftActions(request);

        verify(request).sendResponse(any(Modal.class), eq(MessageMode.USER));
    }

    @Test
    void handleEventDraftActions_editDraftCourses() {
        final var draftId = 1L;
        final var request = mock(ButtonRequest.class);
        final Map<String, Long> arguments = Map.of(DRAFT_ID, draftId);
        final var draft = TestEntities.eventDraftJpa(1);

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventDraftRepository.findById(draftId))
            .thenReturn(Optional.of(draft));
        when(request.getAction())
            .thenReturn(ButtonAction.EDIT_DRAFT_COURSES);
        when(courseService.createCourseSelectionMenu(any(), any()))
            .thenReturn(testCourseSelectMenu());

        eventDraftService.handleEventDraftActions(request);

        verify(request).sendResponse(any(MessageCreateData.class), eq(MessageMode.USER));
    }

    @Test
    void handleEventDraftActions_confirmDraft() {
        final var draftId = 1L;
        final var request = mock(ButtonRequest.class);
        final Map<String, Long> arguments = Map.of(DRAFT_ID, draftId);
        final var draft = TestEntities.eventDraftJpa(1);

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventDraftRepository.findById(1L))
            .thenReturn(Optional.of(draft));
        when(request.getAction())
            .thenReturn(ButtonAction.CONFIRM_DRAFT);

        eventDraftService.handleEventDraftActions(request);

        verify(eventService).publishEvent(request, draft);
    }

    @Test
    void handleEventDraftActions_unsupportedInteraction() {
        final var draftId = 1L;
        final var request = mock(ButtonRequest.class);
        final Map<String, Long> arguments = Map.of(DRAFT_ID, draftId);
        final var draft = TestEntities.eventDraftJpa(1);

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventDraftRepository.findById(1L))
            .thenReturn(Optional.of(draft));
        when(request.getAction())
            .thenReturn(ButtonAction.MARK_COMPLETE);

        assertThatExceptionOfType(IllegalInteractionException.class)
            .isThrownBy(() -> eventDraftService.handleEventDraftActions(request));
    }

    @Test
    void createEventDraft() {
        final var member = mock(Member.class);
        final var memberId = 1234L;
        final var request = mock(CommandRequest.class);
        final Map<String, String> arguments = new HashMap<>();
        arguments.put(DiscordFieldsUtils.DATE, "2025-05-5");
        arguments.put(DiscordFieldsUtils.TIME, "14:30");
        arguments.put(DiscordFieldsUtils.TYPE, EventJpa.EventType.ASSIGNMENT.name());

        when(request.getArguments())
            .thenReturn(arguments);
        when(request.getRequester())
            .thenReturn(member);
        when(member.getIdLong())
            .thenReturn(memberId);
        when(eventDraftRepository.save(any(EventDraftJpa.class)))
            .thenAnswer(invocation -> addIdToDraft(invocation.getArgument(0), 54321L));

        eventDraftService.createEventDraft(request);

        verify(eventDraftRepository).save(argThat(draft ->
            draft.getType().equals(EventJpa.EventType.ASSIGNMENT) &&
            draft.getEventDate().isEqual(LocalDateTime.of(2025, 5, 5, 14, 30))
        ));
        verify(schedulingService).addDraftCleanupTrigger(eq(54321L), any(LocalDateTime.class));
        verify(request).sendResponse(any(Modal.class), eq(MessageMode.USER));
    }

    @Test
    void createEventDraft_invalidEventType() {
        final var request = mock(CommandRequest.class);
        final Map<String, String> arguments = new HashMap<>();
        arguments.put(DiscordFieldsUtils.DATE, "2025-05-5");
        arguments.put(DiscordFieldsUtils.TIME, "14:30");
        arguments.put(DiscordFieldsUtils.TYPE, "INVALID_TYPE");

        when(request.getArguments())
            .thenReturn(arguments);

        eventDraftService.createEventDraft(request);

        verify(request).sendResponse("Something unexpected went wrong. Failed to parse event type.", MessageMode.USER);
        verify(schedulingService, never()).addDraftCleanupTrigger(eq(54321L), any(LocalDateTime.class));
        verify(request, never()).sendResponse(any(Modal.class), eq(MessageMode.USER));
    }

    @Test
    void createEventDraft_invalidDateTime() {
        final var request = mock(CommandRequest.class);
        final Map<String, String> arguments = new HashMap<>();
        arguments.put(DiscordFieldsUtils.DATE, "NOT_A_DATE");
        arguments.put(DiscordFieldsUtils.TIME, "NOT_A_TIME");
        arguments.put(DiscordFieldsUtils.TYPE, EventJpa.EventType.ASSIGNMENT.name());

        when(request.getArguments())
            .thenReturn(arguments);

        eventDraftService.createEventDraft(request);

        verify(request).sendResponse(
            """
                Failed to parse date and time. Reminder:
                %s
                %s
                """.formatted(DateFormatter.DATE_FORMAT_EXAMPLE, DateFormatter.TIME_FORMAT_EXAMPLE),
            MessageMode.USER
        );
        verify(schedulingService, never()).addDraftCleanupTrigger(eq(54321L), any(LocalDateTime.class));
        verify(request, never()).sendResponse(any(Modal.class), eq(MessageMode.USER));
    }

    @Test
    void createEventDraft_dateInPast() {
        final var request = mock(CommandRequest.class);
        final Map<String, String> arguments = new HashMap<>();
        arguments.put(DiscordFieldsUtils.DATE, "1999-5-24");
        arguments.put(DiscordFieldsUtils.TIME, "14:30");
        arguments.put(DiscordFieldsUtils.TYPE, EventJpa.EventType.ASSIGNMENT.name());

        when(request.getArguments())
            .thenReturn(arguments);

        eventDraftService.createEventDraft(request);

        verify(request).sendResponse("Event date cannot be in the past.", MessageMode.USER);
        verify(schedulingService, never()).addDraftCleanupTrigger(eq(54321L), any(LocalDateTime.class));
        verify(request, never()).sendResponse(any(Modal.class), eq(MessageMode.USER));
    }

    @Test
    void addDraftDetails() {
        final var draftId = 1234L;
        final var request = mock(ModalRequest.class);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(DRAFT_ID, draftId);
        final var draft = TestEntities.draftMissingDetailsAndCourses();
        final var jdaEvent = mock(ModalInteractionEvent.class);

        final var title = "Test Title";
        final var note = "Test Note";
        final var titleModalMap = TestEntities.modalMapping(DiscordFieldsUtils.TITLE, title);
        final var noteModalMap = TestEntities.modalMapping(DiscordFieldsUtils.NOTE, note);

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventDraftRepository.findById(draftId))
            .thenReturn(Optional.of(draft));
        when(request.getEvent())
            .thenReturn(jdaEvent);
        when(jdaEvent.getValue(DiscordFieldsUtils.TITLE))
            .thenReturn(titleModalMap);
        when(jdaEvent.getValue(DiscordFieldsUtils.NOTE))
            .thenReturn(noteModalMap);
        when(courseService.createCourseSelectionMenu(any(), any()))
            .thenReturn(testCourseSelectMenu());

        eventDraftService.addDraftDetails(request);

        verify(request).sendResponse(any(MessageCreateData.class), eq(MessageMode.USER));

        assertThat(draft.getTitle()).isEqualTo(title);
        assertThat(draft.getNote()).isEqualTo(note);
    }

    @Test
    void addDraftDetails_noTitle() {
        final var draftId = 1234L;
        final var request = mock(ModalRequest.class);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(DRAFT_ID, draftId);
        final var draft = TestEntities.draftMissingDetailsAndCourses();
        final var jdaEvent = mock(ModalInteractionEvent.class);

        final var titleModalMap = TestEntities.modalMapping(DiscordFieldsUtils.TITLE, "");

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventDraftRepository.findById(draftId))
            .thenReturn(Optional.of(draft));
        when(request.getEvent())
            .thenReturn(jdaEvent);
        when(jdaEvent.getValue(DiscordFieldsUtils.TITLE))
            .thenReturn(titleModalMap);

        assertThatExceptionOfType(DataAccessException.class)
            .isThrownBy(() -> eventDraftService.addDraftDetails(request));
    }

    @Test
    void addDraftDetails_missingNote() {
        final var draftId = 1234L;
        final var request = mock(ModalRequest.class);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(DRAFT_ID, draftId);
        final var draft = TestEntities.draftMissingDetailsAndCourses();
        final var jdaEvent = mock(ModalInteractionEvent.class);

        final var title = "Test Title";
        final var note = "";
        final var titleModalMap = TestEntities.modalMapping(DiscordFieldsUtils.TITLE, title);
        final var noteModalMap = TestEntities.modalMapping(DiscordFieldsUtils.NOTE, note);

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventDraftRepository.findById(draftId))
            .thenReturn(Optional.of(draft));
        when(request.getEvent())
            .thenReturn(jdaEvent);
        when(jdaEvent.getValue(DiscordFieldsUtils.TITLE))
            .thenReturn(titleModalMap);
        when(jdaEvent.getValue(DiscordFieldsUtils.NOTE))
            .thenReturn(noteModalMap);
        when(courseService.createCourseSelectionMenu(any(), any()))
            .thenReturn(testCourseSelectMenu());

        eventDraftService.addDraftDetails(request);

        verify(request).sendResponse(any(MessageCreateData.class), eq(MessageMode.USER));

        assertThat(draft.getTitle()).isEqualTo(title);
        assertThat(draft.getNote()).isNull();
    }

    @Test
    void setCoursesOnDraft() {
        final var draftId = 1234L;
        final var request = mock(SelectionRequest.class);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(DRAFT_ID, draftId);
        final var draft = TestEntities.draftMissingDetailsAndCourses();
        final var jdaEvent = mock(StringSelectInteractionEvent.class);
        final var requester = TestEntities.member(TestEntities.guild());

        final var courses = List.of(TestEntities.courseJpa(1), TestEntities.courseJpa(2), TestEntities.courseJpa(3));

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventDraftRepository.findById(draftId))
            .thenReturn(Optional.of(draft));
        when(request.getEvent())
            .thenReturn(jdaEvent);
        when(jdaEvent.getValues())
            .thenReturn(courses.stream().map(CourseJpa::getCode).toList());
        when(courseService.coursesFromCourseCodes(request.getEvent().getValues()))
            .thenReturn(courses);
        when(configService.getConfigJpa())
            .thenReturn(TestEntities.configJpa());
        when(request.getRequester())
            .thenReturn(requester);

        eventDraftService.setCoursesOnDraft(request);

        assertThat(draft.getCourses()).containsExactlyElementsOf(courses);
        verify(request).sendResponse(any(MessageCreateData.class), eq(MessageMode.USER));
        verify(request, never()).sendResponse(any(String.class), eq(MessageMode.USER));
    }

    @Test
    void setCoursesOnDraft_missingDraft() {
        final var draftId = 1234L;
        final var request = mock(SelectionRequest.class);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(DRAFT_ID, draftId);
        final var jdaEvent = mock(StringSelectInteractionEvent.class);
        final var message = mock(Message.class);
        final AuditableRestAction<Void> restAction = mock(AuditableRestAction.class);

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventDraftRepository.findById(draftId))
            .thenReturn(Optional.empty());
        when(request.getEvent())
            .thenReturn(jdaEvent);
        when(jdaEvent.getMessage())
            .thenReturn(message);
        when(message.delete())
            .thenReturn(restAction);

        eventDraftService.setCoursesOnDraft(request);

        verify(request).sendResponse("Failed to find draft. It may have already been deleted.", MessageMode.USER);
        verify(courseService, never()).coursesFromCourseCodes(any());
    }

    @Test
    void updateDraftDetails() {

    }

    @Test
    void deleteDraft() {

    }

    private EventDraftJpa addIdToDraft(EventDraftJpa draft, Long id) {
        return EventDraftJpa.builder()
            .id(id)
            .authorId(draft.getAuthorId())
            .title(draft.getTitle())
            .note(draft.getNote())
            .eventDate(draft.getEventDate())
            .courses(draft.getCourses())
            .type(draft.getType())
            .draftCreationDate(draft.getDraftCreationDate())
            .build();
    }

    private StringSelectMenu testCourseSelectMenu() {
        List<CourseJpa> courses = List.of(TestEntities.courseJpa(1), TestEntities.courseJpa(2), TestEntities.courseJpa(3));
        List<SelectOption> options = courses.stream()
            .map(course -> SelectOption.of(course.getCode(), course.getCode()).withDescription(course.getName()))
            .toList();
        return StringSelectMenu.create("course-select-test")
            .setPlaceholder("Select Courses")
            .setMaxValues(options.size())
            .addOptions(options)
            .build();
    }

}