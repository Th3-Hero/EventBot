package com.th3hero.eventbot.services;

import com.th3hero.eventbot.TestEntities;
import com.th3hero.eventbot.commands.actions.Command;
import com.th3hero.eventbot.commands.requests.*;
import com.th3hero.eventbot.commands.requests.InteractionRequest.MessageMode;
import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.entities.StudentJpa;
import com.th3hero.eventbot.exceptions.ConfigErrorException;
import com.th3hero.eventbot.exceptions.DataAccessException;
import com.th3hero.eventbot.factories.ButtonFactory;
import com.th3hero.eventbot.formatting.InteractionArguments;
import com.th3hero.eventbot.repositories.EventDraftRepository;
import com.th3hero.eventbot.repositories.EventRepository;
import jakarta.persistence.EntityNotFoundException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

import static com.th3hero.eventbot.TestEntities.TEST_DATE;
import static com.th3hero.eventbot.utils.DiscordFieldsUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private CourseService courseService;
    @Mock
    private ConfigService configService;
    @Mock
    private EventDraftRepository eventDraftRepository;
    @Mock
    private SchedulingService schedulingService;
    @Mock
    private StudentService studentService;

    @InjectMocks
    private EventService eventService;

    @SuppressWarnings("unchecked")
    @Test
    void publishEvent() {
        final var request = mock(ButtonRequest.class);
        final var draft = TestEntities.eventDraftJpa(1);
        final var event = TestEntities.eventJpaWithId(1);
        final var config = TestEntities.configJpa();
        final var jpaEvent = mock(ButtonInteractionEvent.class);
        final var requester = TestEntities.member();
        final var jda = mock(JDA.class);
        final var channel = mock(TextChannel.class);
        final var channelUnion = mock(MessageChannelUnion.class);
        final var messageCreateAction = mock(MessageCreateAction.class);
        final var messageId = 5678L;
        final var message = mock(Message.class);

        when(eventRepository.save(any(EventJpa.class)))
            .thenReturn(event);
        when(configService.getConfigJpa())
            .thenReturn(config);
        when(request.getEvent())
            .thenReturn(jpaEvent);
        when(jpaEvent.getJDA())
            .thenReturn(jda);
        when(jda.getTextChannelById(config.getEventChannel()))
            .thenReturn(channel);
        when(channel.sendMessageEmbeds(any(MessageEmbed.class)))
            .thenReturn(messageCreateAction);
        when(messageCreateAction.addComponents(any(LayoutComponent.class)))
            .thenReturn(messageCreateAction);
        when(message.getIdLong())
            .thenReturn(messageId);
        when(request.getRequester())
            .thenReturn(requester);
        when(message.getChannel())
            .thenReturn(channelUnion);

        eventService.publishEvent(request, draft);

        verify(eventDraftRepository).deleteById(draft.getId());
        verify(schedulingService).removeDraftCleanupTrigger(draft.getId());

        verify(studentService, times(9)).scheduleStudentForEvent(eq(event), any(StudentJpa.class));

        final ArgumentCaptor<Consumer<Message>> messageCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(messageCreateAction).queue(messageCaptor.capture());
        final Consumer<Message> messageConsumer = messageCaptor.getValue();
        messageConsumer.accept(message);

        final ArgumentCaptor<EventJpa> jpaCaptor = ArgumentCaptor.forClass(EventJpa.class);
        verify(eventRepository, times(2)).save(jpaCaptor.capture());
        final EventJpa savedEvent = jpaCaptor.getValue();
        assertThat(savedEvent.getMessageId()).isEqualTo(messageId);

        verify(request).sendResponse("Event has been posted to the event channel. null", MessageMode.USER);
    }

    @Test
    void publishEvent_failedToGetChannel() {
        final var request = mock(ButtonRequest.class);
        final var draft = TestEntities.eventDraftJpa(1);
        final var event = TestEntities.eventJpaWithId(1);
        final var config = TestEntities.configJpa();
        final var jpaEvent = mock(ButtonInteractionEvent.class);
        final var jda = mock(JDA.class);

        when(eventRepository.save(any(EventJpa.class)))
            .thenReturn(event);
        when(configService.getConfigJpa())
            .thenReturn(config);
        when(request.getEvent())
            .thenReturn(jpaEvent);
        when(jpaEvent.getJDA())
            .thenReturn(jda);
        when(jda.getTextChannelById(config.getEventChannel()))
            .thenReturn(null);

        assertThatExceptionOfType(ConfigErrorException.class)
            .isThrownBy(() -> eventService.publishEvent(request, draft));

        verify(eventDraftRepository, never()).deleteById(draft.getId());
        verify(schedulingService, never()).removeDraftCleanupTrigger(draft.getId());
    }

    @Test
    void sendDeleteConformation() {
        final var request = mock(ButtonRequest.class);
        final var requester = TestEntities.member();
        final var event = TestEntities.eventJpaWithId(1);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, event.getId());

        when(request.getRequester())
            .thenReturn(requester);
        doReturn(true)
            .when(requester)
            .hasPermission(Permission.ADMINISTRATOR);
        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.of(event));

        eventService.sendDeleteConformation(request);

        verify(request).sendResponse(any(Modal.class), eq(MessageMode.USER));
    }

    @Test
    void sendDeleteConformation_nonAdmin() {
        final var request = mock(ButtonRequest.class);
        final var requester = TestEntities.member();

        when(request.getRequester())
            .thenReturn(requester);
        doReturn(false)
            .when(requester)
            .hasPermission(Permission.ADMINISTRATOR);

        eventService.sendDeleteConformation(request);

        verify(request).sendResponse("This action requires administrator permissions", MessageMode.USER);
        verify(eventRepository, never()).findById(any());
        verify(request, never()).sendResponse(any(Modal.class), eq(MessageMode.USER));
    }

    @Test
    void sendDeleteConformation_missingEvent() {
        final var request = mock(ButtonRequest.class);
        final var requester = TestEntities.member();
        final var event = TestEntities.eventJpaWithId(1);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, event.getId());

        when(request.getRequester())
            .thenReturn(requester);
        doReturn(true)
            .when(requester)
            .hasPermission(Permission.ADMINISTRATOR);
        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.empty());

        assertThatExceptionOfType(EntityNotFoundException.class)
            .isThrownBy(() -> eventService.sendDeleteConformation(request));

        verify(request, never()).sendResponse(any(Modal.class), eq(MessageMode.USER));
    }

    @Test
    void sendDeleteConformation_deletedEvent() {
        final var request = mock(ButtonRequest.class);
        final var requester = TestEntities.member();
        final var event = TestEntities.eventJpaWithId(1);
        event.setDeleted(true);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, event.getId());

        when(request.getRequester())
            .thenReturn(requester);
        doReturn(true)
            .when(requester)
            .hasPermission(Permission.ADMINISTRATOR);
        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.of(event));

        eventService.sendDeleteConformation(request);

        verify(request).sendResponse("This event has been deleted, actions cannot be preformed on it.", MessageMode.USER);
        verify(request, never()).sendResponse(any(Modal.class), eq(MessageMode.USER));
    }

    @SuppressWarnings("unchecked")
    @Test
    void handleDeleteConformation() {
        final var config = TestEntities.configJpa();
        final var request = mock(ModalRequest.class);
        final var requester = TestEntities.member();
        final var event = TestEntities.eventJpaWithId(1);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, event.getId());
        final var jdaEvent = mock(ModalInteractionEvent.class);
        final var channel = mock(MessageChannelUnion.class);
        final RestAction<Message> messageRestAction = mock(RestAction.class);
        final var message = mock(Message.class);
        final var messageEditAction = mock(MessageEditAction.class);
        final var messageCreateAction = mock(MessageCreateAction.class);

        final var reason = "The assignment was cancelled";
        final var reasonModalMap = TestEntities.modalMapping(REASON, reason);

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.of(event));
        when(request.getEvent())
            .thenReturn(jdaEvent);
        when(jdaEvent.getValue(REASON))
            .thenReturn(reasonModalMap);
        when(configService.getConfigJpa())
            .thenReturn(config);
        when(jdaEvent.getChannel())
            .thenReturn(channel);
        when(channel.retrieveMessageById(event.getMessageId()))
            .thenReturn(messageRestAction);
        when(message.editMessageComponents())
            .thenReturn(messageEditAction);
        when(request.getRequester())
            .thenReturn(requester);
        when(channel.sendMessageEmbeds(any(MessageEmbed.class)))
            .thenReturn(messageCreateAction);
        when(messageCreateAction.addActionRow(any(Button.class)))
            .thenReturn(messageCreateAction);


        eventService.handleDeleteConformation(request);

        final ArgumentCaptor<Consumer<Message>> messageCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(messageRestAction).queue(messageCaptor.capture(), any());
        final Consumer<Message> messageValue = messageCaptor.getValue();
        messageValue.accept(message);
        verify(messageEditAction).queue();

        verify(schedulingService).removeReminderTriggers(event.getId());
        verify(eventRepository).save(argThat(EventJpa::getDeleted));

        final ArgumentCaptor<Consumer<Message>> recoveryMessageCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(messageCreateAction).queue(recoveryMessageCaptor.capture());
        final Consumer<Message> recoveryMessageValue = recoveryMessageCaptor.getValue();
        recoveryMessageValue.accept(message);
        verify(schedulingService).addDeletedEventCleanupTrigger(eq(event.getId()), anyLong(), any(LocalDateTime.class));

        verify(request).sendResponse("Event has been deleted. null", MessageMode.USER);
    }

    @Test
    void handleDeleteConformation_missingEvent() {
        final var request = mock(ModalRequest.class);
        final var event = TestEntities.eventJpaWithId(1);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, event.getId());
        final var jdaEvent = mock(ModalInteractionEvent.class);

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.empty());
        when(request.getEvent())
            .thenReturn(jdaEvent);
        when(jdaEvent.getModalId())
            .thenReturn("modalId");

        assertThatExceptionOfType(EntityNotFoundException.class)
            .isThrownBy(() -> eventService.handleDeleteConformation(request));
    }

    @Test
    void handleDeleteConformation_eventAlreadyDeleted() {
        final var request = mock(ModalRequest.class);
        final var event = TestEntities.eventJpaWithId(1);
        event.setDeleted(true);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, event.getId());

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.of(event));

        eventService.handleDeleteConformation(request);

        verify(request).sendResponse("Event is already deleted.", MessageMode.USER);
        verify(request, never()).getEvent();
        verify(configService, never()).getConfigJpa();
    }

    @Test
    void handleDeleteConformation_failedToGetReason() {
        final var request = mock(ModalRequest.class);
        final var event = TestEntities.eventJpaWithId(1);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, event.getId());
        final var jdaEvent = mock(ModalInteractionEvent.class);

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.of(event));
        when(request.getEvent())
            .thenReturn(jdaEvent);
        when(jdaEvent.getValue(REASON))
            .thenReturn(null);

        assertThatExceptionOfType(DataAccessException.class)
            .isThrownBy(() -> eventService.handleDeleteConformation(request));
    }

    @SuppressWarnings("unchecked")
    @Test
    void undoEventDeletion() {
        final var request = mock(ButtonRequest.class);
        final var requester = TestEntities.member();
        final var event = TestEntities.eventJpaWithId(1);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, event.getId());
        final var jdaEvent = mock(ButtonInteractionEvent.class);
        final var message = mock(Message.class);
        final AuditableRestAction<Void> restAction = mock(AuditableRestAction.class);
        final var channel = mock(MessageChannelUnion.class);
        final RestAction<Message> messageRestAction = mock(RestAction.class);
        final var messageCreateAction = mock(MessageCreateAction.class);
        final var messageEditAction = mock(MessageEditAction.class);

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.of(event));
        when(request.getEvent())
            .thenReturn(jdaEvent);
        when(jdaEvent.getMessage())
            .thenReturn(message);
        when(message.delete())
            .thenReturn(restAction);
        when(jdaEvent.getChannel())
            .thenReturn(channel);
        when(channel.retrieveMessageById(event.getMessageId()))
            .thenReturn(messageRestAction);
        when(request.getRequester())
            .thenReturn(requester);
        when(message.replyEmbeds(any(MessageEmbed.class)))
            .thenReturn(messageCreateAction);
        when(message.editMessageComponents(any(ActionRow.class)))
            .thenReturn(messageEditAction);

        eventService.undoEventDeletion(request);

        verify(schedulingService).removeDeletedEventCleanupTrigger(event.getId());
        verify(request).sendResponse("Event has been restored.", MessageMode.USER);

        final ArgumentCaptor<Consumer<Message>> messageCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(messageRestAction).queue(messageCaptor.capture(), any());
        final Consumer<Message> messageValue = messageCaptor.getValue();
        messageValue.accept(message);

        verify(message).replyEmbeds(any(MessageEmbed.class));
        verify(message).editMessageComponents(ButtonFactory.eventButtons(event.getId()));
    }

    @Test
    void undoEventDeletion_missingEvent() {
        final var request = mock(ButtonRequest.class);
        final var event = TestEntities.eventJpaWithId(1);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, event.getId());

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.empty());

        assertThatExceptionOfType(EntityNotFoundException.class)
            .isThrownBy(() -> eventService.undoEventDeletion(request));

        verify(request, never()).sendResponse(anyString(), any());
        verify(schedulingService, never()).removeDeletedEventCleanupTrigger(any());
    }

    @Test
    void markEventComplete() {
        final var request = mock(ButtonRequest.class);
        final var event = TestEntities.eventJpaWithId(1);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, event.getId());

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.existsByIdAndDeletedIsFalse(event.getId()))
            .thenReturn(true);

        eventService.markEventComplete(request);

        verify(studentService).unscheduleStudentRemindersForEvent(request, event.getId());
    }

    @Test
    void markEventComplete_missingEvent() {
        final var request = mock(ButtonRequest.class);
        final var eventId = 1234L;
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, eventId);

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.existsByIdAndDeletedIsFalse(eventId))
            .thenReturn(false);

        eventService.markEventComplete(request);

        verify(request).sendResponse("Failed to find event %s".formatted(eventId), MessageMode.USER);
    }

    @Test
    void sendEventEditOptions() {
        final var request = mock(ButtonRequest.class);
        final var event = TestEntities.eventJpaWithId(1);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, event.getId());

        adminMocks(request);
        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.of(event));

        eventService.sendEventEditOptions(request);

        verify(request).sendResponse(any(MessageCreateData.class), eq(MessageMode.USER));
    }

    @Test
    void sendEventEditOptions_notAdmin() {
        final var request = mock(ButtonRequest.class);

        nonAdminMocks(request);
        eventService.sendEventEditOptions(request);
        nonAdminVerify(request);

        verify(request, never()).sendResponse(any(MessageCreateData.class), eq(MessageMode.USER));
    }

    @Test
    void sendEventEditOptions_missingEvent() {
        final var request = mock(ButtonRequest.class);
        final var event = TestEntities.eventJpaWithId(1);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, event.getId());

        adminMocks(request);
        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.empty());

        assertThatExceptionOfType(EntityNotFoundException.class)
            .isThrownBy(() -> eventService.sendEventEditOptions(request));

        verify(request, never()).sendResponse(any(MessageCreateData.class), eq(MessageMode.USER));
    }

    @Test
    void sendEventEditOptions_eventDeleted() {
        final var request = mock(ButtonRequest.class);
        final var event = TestEntities.eventJpaWithId(1);
        event.setDeleted(true);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, event.getId());

        adminMocks(request);
        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.of(event));

        eventService.sendEventEditOptions(request);

        verify(request).sendResponse("This event has been deleted, actions cannot be preformed on it.", MessageMode.USER);
        verify(request, never()).sendResponse(any(MessageCreateData.class), eq(MessageMode.USER));
    }

    @Test
    void sendEditEventDetails() {
        final var request = mock(ButtonRequest.class);
        final var event = TestEntities.eventJpaWithId(1);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, event.getId());

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.of(event));

        eventService.sendEditEventDetails(request);

        verify(request).sendResponse(any(Modal.class), eq(MessageMode.USER));
    }

    @Test
    void sendEditEventDetails_missingEvent() {
        final var request = mock(ButtonRequest.class);
        final var eventId = 1234L;
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, eventId);

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.findById(eventId))
            .thenReturn(Optional.empty());

        assertThatExceptionOfType(EntityNotFoundException.class)
            .isThrownBy(() -> eventService.sendEditEventDetails(request));

        verify(request, never()).sendResponse(any(Modal.class), eq(MessageMode.USER));
    }

    @Test
    void sendEditEventDetails_deletedEvent() {
        final var request = mock(ButtonRequest.class);
        final var event = TestEntities.eventJpaWithId(1);
        event.setDeleted(true);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, event.getId());

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.of(event));

        eventService.sendEditEventDetails(request);

        verify(request).sendResponse("This event has been deleted, actions cannot be preformed on it.", MessageMode.USER);
        verify(request, never()).sendResponse(any(Modal.class), eq(MessageMode.USER));
    }

    @Test
    void sendEventEditCourses() {
        final var request = mock(ButtonRequest.class);
        final var event = TestEntities.eventJpaWithId(1);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, event.getId());
        final var menu = TestEntities.courseSelectMenu();

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.of(event));
        when(courseService.createCourseSelectionMenu(any(), eq(event.getCourses())))
            .thenReturn(menu);

        eventService.sendEventEditCourses(request);

        verify(request).sendResponse(any(MessageCreateData.class), eq(MessageMode.USER));
    }

    @Test
    void sendEventEditCourses_missingEvents() {
        final var request = mock(ButtonRequest.class);
        final var eventId = 1234L;
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, eventId);

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.findById(eventId))
            .thenReturn(Optional.empty());

        assertThatExceptionOfType(EntityNotFoundException.class)
            .isThrownBy(() -> eventService.sendEventEditCourses(request));

        verify(request, never()).sendResponse(any(MessageCreateData.class), eq(MessageMode.USER));
    }

    @Test
    void sendEventEditCourses_deletedEvent() {
        final var request = mock(ButtonRequest.class);
        final var event = TestEntities.eventJpaWithId(1);
        event.setDeleted(true);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, event.getId());

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.of(event));

        eventService.sendEventEditCourses(request);

        verify(request).sendResponse("This event has been deleted, actions cannot be preformed on it.", MessageMode.USER);
        verify(request, never()).sendResponse(any(MessageCreateData.class), eq(MessageMode.USER));
    }

    @SuppressWarnings("unchecked")
    @Test
    void editEventDetails() {
        final var request = mock(ModalRequest.class);
        final var requester = TestEntities.member();
        final var event = TestEntities.eventJpaWithId(1);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, event.getId());
        final var jdaEvent = mock(ModalInteractionEvent.class);
        final var channel = mock(MessageChannelUnion.class);
        final RestAction<Message> messageRestAction = mock(RestAction.class);
        final var message = mock(Message.class);
        final var messageCreateAction = mock(MessageCreateAction.class);
        final var messageEditAction = mock(MessageEditAction.class);


        final var title = "New Title";
        final var note = "New Note";
        final var date = "2025-05-25";
        final var time = "16:35";

        final var titleModalMap = TestEntities.modalMapping(TITLE, title);
        final var noteModalMap = TestEntities.modalMapping(NOTE, note);
        final var dateModalMap = TestEntities.modalMapping(DATE, date);
        final var timeModalMap = TestEntities.modalMapping(TIME, time);

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.of(event));
        when(request.getEvent())
            .thenReturn(jdaEvent);
        when(jdaEvent.getValue(TITLE))
            .thenReturn(titleModalMap);
        when(jdaEvent.getValue(NOTE))
            .thenReturn(noteModalMap);
        when(jdaEvent.getValue(DATE))
            .thenReturn(dateModalMap);
        when(jdaEvent.getValue(TIME))
            .thenReturn(timeModalMap);
        when(jdaEvent.getChannel())
            .thenReturn(channel);
        when(request.getRequester())
            .thenReturn(requester);
        when(eventRepository.save(any(EventJpa.class)))
            .thenAnswer(i -> i.getArgument(0));
        when(channel.retrieveMessageById(event.getMessageId()))
            .thenReturn(messageRestAction);
        when(message.replyEmbeds(any(MessageEmbed.class)))
            .thenReturn(messageCreateAction);
        when(message.editMessageEmbeds(any(MessageEmbed.class)))
            .thenReturn(messageEditAction);

        eventService.editEventDetails(request);

        verify(eventRepository).save(argThat(e ->
            e.getTitle().equals(title) &&
                e.getNote().equals(note) &&
                e.getEventDate().equals(LocalDateTime.of(2025, 5, 25, 16, 35))
        ));

        verify(schedulingService).removeReminderTriggers(event.getId());
        verify(request, never()).sendResponse("Failed to parse date and time", MessageMode.USER);

        final ArgumentCaptor<Consumer<Message>> messageCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(messageRestAction).queue(messageCaptor.capture(), any());
        final Consumer<Message> messageConsumer = messageCaptor.getValue();
        messageConsumer.accept(message);

        verify(message).editMessageEmbeds(any(MessageEmbed.class));
        verify(message).replyEmbeds(any(MessageEmbed.class));

        verify(request).sendResponse("The event has been updated. null", MessageMode.USER);
    }

    @Test
    void editEventDetails_dateNotChanged() {
        final var request = mock(ModalRequest.class);
        final var requester = TestEntities.member();
        final var event = TestEntities.eventJpaWithId(1);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, event.getId());
        final var jdaEvent = mock(ModalInteractionEvent.class);
        final var channel = mock(MessageChannelUnion.class);
        final RestAction<Message> messageRestAction = mock(RestAction.class);

        final var title = "New Title";
        final var note = "New Note";
        final var date = "2099-01-01";
        final var time = "1:01";

        final var titleModalMap = TestEntities.modalMapping(TITLE, title);
        final var noteModalMap = TestEntities.modalMapping(NOTE, note);
        final var dateModalMap = TestEntities.modalMapping(DATE, date);
        final var timeModalMap = TestEntities.modalMapping(TIME, time);

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.of(event));
        when(request.getEvent())
            .thenReturn(jdaEvent);
        when(jdaEvent.getValue(TITLE))
            .thenReturn(titleModalMap);
        when(jdaEvent.getValue(NOTE))
            .thenReturn(noteModalMap);
        when(jdaEvent.getValue(DATE))
            .thenReturn(dateModalMap);
        when(jdaEvent.getValue(TIME))
            .thenReturn(timeModalMap);
        when(jdaEvent.getChannel())
            .thenReturn(channel);
        when(request.getRequester())
            .thenReturn(requester);
        when(eventRepository.save(any(EventJpa.class)))
            .thenAnswer(i -> i.getArgument(0));
        when(channel.retrieveMessageById(event.getMessageId()))
            .thenReturn(messageRestAction);

        eventService.editEventDetails(request);

        verify(eventRepository).save(argThat(e ->
            e.getTitle().equals(title) &&
                e.getNote().equals(note) &&
                e.getEventDate().equals(TEST_DATE)
        ));

        verify(schedulingService, never()).removeReminderTriggers(event.getId());
        verify(request, never()).sendResponse("Failed to parse date and time", MessageMode.USER);
    }

    @Test
    void editEventDetails_missingEvent() {
        final var request = mock(ModalRequest.class);
        final var event = TestEntities.eventJpaWithId(1);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, event.getId());

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.empty());

        assertThatExceptionOfType(EntityNotFoundException.class)
            .isThrownBy(() -> eventService.editEventDetails(request));

        verify(request, never()).sendResponse(any(), any());
    }

    @Test
    void editEventDetails_missingTitle() {
        final var request = mock(ModalRequest.class);
        final var event = TestEntities.eventJpaWithId(1);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, event.getId());
        final var jdaEvent = mock(ModalInteractionEvent.class);

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.of(event));
        when(request.getEvent())
            .thenReturn(jdaEvent);
        when(jdaEvent.getValue(TITLE))
            .thenReturn(null);

        assertThatExceptionOfType(DataAccessException.class)
            .isThrownBy(() -> eventService.editEventDetails(request))
            .withMessage("Failed to parse title from modal");

        verify(request, never()).sendResponse(any(), any());
    }

    @Test
    void editEventDetails_missingNote() {
        final var request = mock(ModalRequest.class);
        final var requester = TestEntities.member();
        final var event = TestEntities.eventJpaWithId(1);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, event.getId());
        final var jdaEvent = mock(ModalInteractionEvent.class);
        final var channel = mock(MessageChannelUnion.class);
        final RestAction<Message> messageRestAction = mock(RestAction.class);


        final var title = "New Title";
        final var date = "2025-05-25";
        final var time = "16:35";

        final var titleModalMap = TestEntities.modalMapping(TITLE, title);
        final var dateModalMap = TestEntities.modalMapping(DATE, date);
        final var timeModalMap = TestEntities.modalMapping(TIME, time);

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.of(event));
        when(request.getEvent())
            .thenReturn(jdaEvent);
        when(jdaEvent.getValue(TITLE))
            .thenReturn(titleModalMap);
        when(jdaEvent.getValue(NOTE))
            .thenReturn(null);
        when(jdaEvent.getValue(DATE))
            .thenReturn(dateModalMap);
        when(jdaEvent.getValue(TIME))
            .thenReturn(timeModalMap);
        when(jdaEvent.getChannel())
            .thenReturn(channel);
        when(request.getRequester())
            .thenReturn(requester);
        when(eventRepository.save(any(EventJpa.class)))
            .thenAnswer(i -> i.getArgument(0));
        when(channel.retrieveMessageById(event.getMessageId()))
            .thenReturn(messageRestAction);

        eventService.editEventDetails(request);

        verify(eventRepository).save(argThat(e ->
            e.getTitle().equals(title) &&
                e.getNote() == null &&
                e.getEventDate().equals(LocalDateTime.of(2025, 5, 25, 16, 35))
        ));

        verify(schedulingService).removeReminderTriggers(event.getId());
        verify(request, never()).sendResponse("Failed to parse date and time", MessageMode.USER);
    }

    @Test
    void editEventDetails_missingDate() {
        final var request = mock(ModalRequest.class);
        final var event = TestEntities.eventJpaWithId(1);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, event.getId());
        final var jdaEvent = mock(ModalInteractionEvent.class);

        final var title = "New Title";
        final var note = "New Note";

        final var titleModalMap = TestEntities.modalMapping(TITLE, title);
        final var noteModalMap = TestEntities.modalMapping(NOTE, note);

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.of(event));
        when(request.getEvent())
            .thenReturn(jdaEvent);
        when(jdaEvent.getValue(TITLE))
            .thenReturn(titleModalMap);
        when(jdaEvent.getValue(NOTE))
            .thenReturn(noteModalMap);
        when(jdaEvent.getValue(DATE))
            .thenReturn(null);

        assertThatExceptionOfType(DataAccessException.class)
            .isThrownBy(() -> eventService.editEventDetails(request))
            .withMessage("Failed to parse date from modal");

        verify(request, never()).sendResponse(any(), any());
    }

    @Test
    void editEventDetails_missingTime() {
        final var request = mock(ModalRequest.class);
        final var event = TestEntities.eventJpaWithId(1);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, event.getId());
        final var jdaEvent = mock(ModalInteractionEvent.class);

        final var title = "New Title";
        final var note = "New Note";
        final var date = "2025-05-25";

        final var titleModalMap = TestEntities.modalMapping(TITLE, title);
        final var noteModalMap = TestEntities.modalMapping(NOTE, note);
        final var dateModalMap = TestEntities.modalMapping(DATE, date);

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.of(event));
        when(request.getEvent())
            .thenReturn(jdaEvent);
        when(jdaEvent.getValue(TITLE))
            .thenReturn(titleModalMap);
        when(jdaEvent.getValue(NOTE))
            .thenReturn(noteModalMap);
        when(jdaEvent.getValue(DATE))
            .thenReturn(dateModalMap);
        when(jdaEvent.getValue(TIME))
            .thenReturn(null);

        assertThatExceptionOfType(DataAccessException.class)
            .isThrownBy(() -> eventService.editEventDetails(request))
            .withMessage("Failed to parse time from modal");

        verify(request, never()).sendResponse(any(), any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void editEventCourses() {
        final var request = mock(SelectionRequest.class);
        final var requester = TestEntities.member();
        final var event = TestEntities.eventJpaWithId(1);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, event.getId());
        final var jdaEvent = mock(StringSelectInteractionEvent.class);
        final var courses = List.of(TestEntities.courseJpa(1), TestEntities.courseJpa(2), TestEntities.courseJpa(3));
        final var channel = mock(MessageChannelUnion.class);
        final RestAction<Message> messageRestAction = mock(RestAction.class);
        final var message = mock(Message.class);
        final var messageCreateAction = mock(MessageCreateAction.class);
        final var messageEditAction = mock(MessageEditAction.class);

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.of(event));
        when(request.getEvent())
            .thenReturn(jdaEvent);
        when(jdaEvent.getValues())
            .thenReturn(List.of());
        when(courseService.coursesFromCourseCodes(any()))
            .thenReturn(courses);
        when(jdaEvent.getChannel())
            .thenReturn(channel);
        when(request.getRequester())
            .thenReturn(requester);
        when(channel.retrieveMessageById(event.getMessageId()))
            .thenReturn(messageRestAction);
        when(message.replyEmbeds(any(MessageEmbed.class)))
            .thenReturn(messageCreateAction);
        when(message.editMessageEmbeds(any(MessageEmbed.class)))
            .thenReturn(messageEditAction);

        eventService.editEventCourses(request);

        assertThat(event.getCourses()).containsExactlyInAnyOrderElementsOf(courses);

        verify(schedulingService).removeReminderTriggers(event.getId());

        final ArgumentCaptor<Consumer<Message>> messageCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(messageRestAction).queue(messageCaptor.capture(), any());
        final Consumer<Message> messageConsumer = messageCaptor.getValue();
        messageConsumer.accept(message);

        verify(message).editMessageEmbeds(any(MessageEmbed.class));
        verify(message).replyEmbeds(any(MessageEmbed.class));

        verify(request).sendResponse("The event has been updated. null", MessageMode.USER);
    }

    @Test
    void editEventCourses_missingEvent() {
        final var request = mock(SelectionRequest.class);
        final var event = TestEntities.eventJpaWithId(1);
        final Map<String, Long> arguments = new HashMap<>();
        arguments.put(InteractionArguments.EVENT_ID, event.getId());

        when(request.getArguments())
            .thenReturn(arguments);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.empty());

        assertThatExceptionOfType(EntityNotFoundException.class)
            .isThrownBy(() -> eventService.editEventCourses(request));

        verify(schedulingService, never()).removeReminderTriggers(event.getId());
    }

    @SuppressWarnings("unchecked")
    @Test
    void filterViewEvents_noMatchingEvents() {
        final var request = mock(CommandRequest.class);
        final var requester = TestEntities.member();
        final var course = TestEntities.courseJpa(1);
        final Map<String, String> arguments = new HashMap<>();
        arguments.put(TIME_PERIOD, "15");
        arguments.put(UPCOMING, "3");
        arguments.put(COURSE, course.getCode());

        final var student = TestEntities.studentJpa(1, List.of(TestEntities.courseJpa(1), TestEntities.courseJpa(2)));

        when(request.getRequester())
            .thenReturn(requester);
        when(studentService.fetchStudent(requester.getIdLong()))
            .thenReturn(student);
        when(request.getArguments())
            .thenReturn(arguments);
        when(courseService.coursesFromCourseCodes(List.of(course.getCode())))
            .thenReturn(List.of(course));
        when(eventRepository.findBy(any(Specification.class), any()))
            .thenReturn(List.of());

        eventService.filterViewEvents(request);

        verify(request).deferReply(MessageMode.USER);
        verify(request).sendResponse("No events found matching the criteria.", MessageMode.USER);

    }

    @Test
    void filterViewEvents_studentHasNoCourses() {
        final var request = mock(CommandRequest.class);
        final var requester = TestEntities.member();
        final var student = TestEntities.studentJpa(1, List.of());

        when(request.getRequester())
            .thenReturn(requester);
        when(studentService.fetchStudent(requester.getIdLong()))
            .thenReturn(student);

        eventService.filterViewEvents(request);

        verify(request).sendResponse(
            "You are not signed up for any courses with the bot. Please use `/%s`".formatted(Command.SELECT_COURSES.getDisplayName()),
            MessageMode.USER
        );
        verify(request, never()).deferReply(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void sendAllEventsToEventChannel() {
        final var jda = mock(JDA.class);
        final var config = TestEntities.configJpa();
        final var channel = mock(TextChannel.class);
        final var messageCreateAction = mock(MessageCreateAction.class);
        final var events = List.of(TestEntities.eventJpaWithId(1), TestEntities.eventJpaWithId(2), TestEntities.eventJpaWithId(3));
        final var message = mock(Message.class);
        final var messageIds = List.of(123L, 456L, 789L);
        final var channelUnion = mock(MessageChannelUnion.class);

        when(configService.getConfigJpa())
            .thenReturn(config);
        when(jda.getTextChannelById(config.getEventChannel()))
            .thenReturn(channel);
        when(channel.sendMessage(anyString()))
            .thenReturn(messageCreateAction);
        when(eventRepository.findAllByDeletedIsFalse())
            .thenReturn(events);
        when(channel.sendMessageEmbeds(any(MessageEmbed.class)))
            .thenReturn(messageCreateAction);
        when(messageCreateAction.addComponents(any(ActionRow.class)))
            .thenReturn(messageCreateAction);
        when(message.getChannel())
            .thenReturn(channelUnion);
        when(message.getIdLong())
            .thenReturn(messageIds.get(0), messageIds.get(1), messageIds.get(2));

        eventService.sendAllEventsToEventChannel(jda);

        verify(channel).sendMessage("This channel is now the event channel. All events will be posted here going forward.");
        verify(channel, times(events.size())).sendMessageEmbeds(any(MessageEmbed.class));

        final ArgumentCaptor<Consumer<Message>> messageCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(messageCreateAction, times(events.size())).queue(messageCaptor.capture(), any());
        final List<Consumer<Message>> messageConsumers = messageCaptor.getAllValues();
        for (int i = 0; i < messageConsumers.size(); i++) {
            final Consumer<Message> messageConsumer = messageConsumers.get(i);
            messageConsumer.accept(message);
            int finalI = i;
            verify(eventRepository).save(argThat(e -> e.getMessageId().equals(messageIds.get(finalI))));
        }
    }

    @Test
    void sendAllEventsToEventChannel_failedToGetChannel() {
        final var jda = mock(JDA.class);
        final var config = TestEntities.configJpa();

        when(configService.getConfigJpa())
            .thenReturn(config);
        when(jda.getTextChannelById(config.getEventChannel()))
            .thenReturn(null);

        eventService.sendAllEventsToEventChannel(jda);

        verify(eventRepository, never()).findAllByDeletedIsFalse();
    }

    private void adminMocks(InteractionRequest request) {
        final var requester = TestEntities.member();
        when(request.getRequester())
            .thenReturn(requester);
        doReturn(true)
            .when(requester)
            .hasPermission(Permission.ADMINISTRATOR);
    }

    private void nonAdminMocks(InteractionRequest request) {
        final var requester = TestEntities.member();
        when(request.getRequester())
            .thenReturn(requester);
        doReturn(false)
            .when(requester)
            .hasPermission(Permission.ADMINISTRATOR);
    }

    private void nonAdminVerify(InteractionRequest request) {
        verify(request).sendResponse("This action requires administrator permissions", MessageMode.USER);
    }

    private List<EventJpa> filterTestList(CourseJpa course, LocalDateTime minTime) {
        // before min
        final var eventOne = createEvent(1, course);
        eventOne.setEventDate(minTime.minusDays(1));
        // after min and before max
        final var eventTwo = createEvent(2, course);
        eventTwo.setEventDate(minTime.plusDays(1));
        final var eventThree = createEvent(3, course);
        eventThree.setEventDate(minTime.plusDays(3));
        final var eventFour = createEvent(4, course);
        eventFour.setEventDate(minTime.plusDays(5));
        // after min and max
        final var eventFive = createEvent(5, course);
        eventFive.setEventDate(minTime.plusDays(20));
        return List.of(eventOne, eventTwo, eventThree, eventFour, eventFive);
    }

    private EventJpa createEvent(int seed, CourseJpa course) {
        return EventJpa.builder()
            .authorId(1234L + seed)
            .messageId(1234L + seed)
            .title("Test Event%s".formatted(seed))
            .note("Test Note%s".formatted(seed))
            .eventDate(TEST_DATE)
            .type(EventJpa.EventType.ASSIGNMENT)
            .courses(new ArrayList<>(List.of(course)))
            .build();
    }
}