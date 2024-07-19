package com.th3hero.eventbot.services;

import com.th3hero.eventbot.TestEntities;
import com.th3hero.eventbot.commands.requests.CommandRequest;
import com.th3hero.eventbot.commands.requests.InteractionRequest.MessageMode;
import com.th3hero.eventbot.config.SlashCommandConfig;
import com.th3hero.eventbot.entities.ConfigJpa;
import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.entities.EventJpa.EventStatus;
import com.th3hero.eventbot.formatting.DateFormatter;
import com.th3hero.eventbot.repositories.ConfigRepository;
import com.th3hero.eventbot.repositories.CourseRepository;
import com.th3hero.eventbot.repositories.EventRepository;
import com.th3hero.eventbot.repositories.StudentRepository;
import jakarta.persistence.EntityManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.th3hero.eventbot.TestEntities.TEST_DATE;
import static com.th3hero.eventbot.utils.DiscordFieldsUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Transactional
@SpringBootTest
@ActiveProfiles("test")
class EventServiceIT {

    @MockBean
    private JDA jda;
    @MockBean
    private SlashCommandConfig slashCommandConfig;

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private ConfigRepository configRepository;

    @Autowired
    private EventService eventService;

    @SuppressWarnings("unchecked")
    @Test
    void filterViewEvents() {
        final var config = TestEntities.configJpa();
        configRepository.saveAndFlush(config);
        final var courseOne = TestEntities.courseJpa(1);
        final var courseTwo = TestEntities.courseJpa(2);
        final var courseThree = TestEntities.courseJpa(3);
        courseRepository.saveAllAndFlush(List.of(courseOne, courseTwo, courseThree));
        final var student = TestEntities.studentJpa(1, List.of(courseOne, courseTwo));
        studentRepository.saveAndFlush(student);

        final var currentDate = LocalDateTime.now();
        // before min
        final var eventOne = createEvent(1, courseOne);
        eventOne.setEventDate(currentDate.minusDays(1));
        // after min and before max
        final var eventTwo = createEvent(2, courseOne);
        eventTwo.setEventDate(currentDate.plusDays(1));
        final var eventThree = createEvent(3, courseOne);
        eventThree.setEventDate(currentDate.plusDays(2));
        final var eventFour = createEvent(4, courseOne);
        eventFour.setEventDate(currentDate.plusDays(3));
        // after min and max
        final var eventFive = createEvent(5, courseOne);
        eventFive.setEventDate(currentDate.plusDays(20));
        // doesn't match course
        final var eventSix = createEvent(6, courseTwo);
        eventSix.setEventDate(currentDate.plusDays(1));
        final var events = List.of(eventOne, eventTwo, eventThree, eventFour, eventFive, eventSix);
        eventRepository.saveAllAndFlush(events);
        entityManager.clear();

        final var request = mock(CommandRequest.class);
        final var requester = TestEntities.member();
        final Map<String, String> arguments = new HashMap<>();
        arguments.put(TIME_PERIOD, "15");
        arguments.put(UPCOMING, "2");
        arguments.put(COURSE, courseOne.getCode());
        final var jdaEvent = mock(SlashCommandInteractionEvent.class);
        final var channel = mock(TextChannel.class);
        final RestAction<Message> restAction = mock(RestAction.class);
        final var message = mock(Message.class);
        final var future = CompletableFuture.completedFuture(message);

        when(request.getRequester())
            .thenReturn(requester);
        when(requester.getIdLong())
            .thenReturn(student.getId());
        when(request.getArguments())
            .thenReturn(arguments);

        when(request.getEvent())
            .thenReturn(jdaEvent);
        when(jdaEvent.getJDA())
            .thenReturn(jda);
        when(jda.getTextChannelById(config.getEventChannel()))
            .thenReturn(channel);
        when(channel.retrieveMessageById(anyLong()))
            .thenReturn(restAction);
        when(restAction.submit())
            .thenReturn(future);

        eventService.filterViewEvents(request);

        verify(request).deferReply(MessageMode.USER);
        verify(request, never()).sendResponse(anyString(), any());
        final ArgumentCaptor<MessageEmbed> embedArgumentCaptor = ArgumentCaptor.forClass(MessageEmbed.class);
        verify(request).sendResponse(embedArgumentCaptor.capture(), eq(MessageMode.USER));
        final var embed = embedArgumentCaptor.getValue();

        assertThat(embed.getFields()).hasSize(2);
        assertThat(embed.getFields().get(0).getName()).isEqualTo(eventTwo.getTitle());
        assertThat(embed.getFields().get(0).getValue()).isEqualTo(summary(eventTwo, config));
        assertThat(embed.getFields().get(1).getName()).isEqualTo(eventThree.getTitle());
        assertThat(embed.getFields().get(1).getValue()).isEqualTo(summary(eventThree, config));
    }

    @SuppressWarnings("unchecked")
    @Test
    void filterViewEvents_noTimePeriodSpecified() {
        final var config = TestEntities.configJpa();
        configRepository.saveAndFlush(config);
        final var courseOne = TestEntities.courseJpa(1);
        final var courseTwo = TestEntities.courseJpa(2);
        final var courseThree = TestEntities.courseJpa(3);
        courseRepository.saveAllAndFlush(List.of(courseOne, courseTwo, courseThree));
        final var student = TestEntities.studentJpa(1, List.of(courseOne, courseTwo));
        studentRepository.saveAndFlush(student);

        final var currentDate = LocalDateTime.now();
        // before now
        final var eventOne = createEvent(1, courseOne);
        eventOne.setEventDate(currentDate.minusDays(1));
        // after now
        final var eventTwo = createEvent(2, courseOne);
        eventTwo.setEventDate(currentDate.plusDays(1));
        final var eventThree = createEvent(3, courseOne);
        eventThree.setEventDate(currentDate.plusDays(20));
        final var eventFour = createEvent(4, courseOne);
        eventFour.setEventDate(currentDate.plusDays(30));
        // doesn't match course
        final var eventFive = createEvent(5, courseTwo);
        eventFive.setEventDate(currentDate.plusDays(1));
        final var events = List.of(eventOne, eventTwo, eventThree, eventFour, eventFive);
        eventRepository.saveAllAndFlush(events);
        entityManager.clear();

        final var request = mock(CommandRequest.class);
        final var requester = TestEntities.member();
        final Map<String, String> arguments = new HashMap<>();
        arguments.put(UPCOMING, "2");
        arguments.put(COURSE, courseOne.getCode());
        final var jdaEvent = mock(SlashCommandInteractionEvent.class);
        final var channel = mock(TextChannel.class);
        final RestAction<Message> restAction = mock(RestAction.class);
        final var message = mock(Message.class);
        final var future = CompletableFuture.completedFuture(message);

        when(request.getRequester())
            .thenReturn(requester);
        when(requester.getIdLong())
            .thenReturn(student.getId());
        when(request.getArguments())
            .thenReturn(arguments);

        when(request.getEvent())
            .thenReturn(jdaEvent);
        when(jdaEvent.getJDA())
            .thenReturn(jda);
        when(jda.getTextChannelById(config.getEventChannel()))
            .thenReturn(channel);
        when(channel.retrieveMessageById(anyLong()))
            .thenReturn(restAction);
        when(restAction.submit())
            .thenReturn(future);

        eventService.filterViewEvents(request);

        verify(request).deferReply(MessageMode.USER);
        verify(request, never()).sendResponse(anyString(), any());
        final ArgumentCaptor<MessageEmbed> embedArgumentCaptor = ArgumentCaptor.forClass(MessageEmbed.class);
        verify(request).sendResponse(embedArgumentCaptor.capture(), eq(MessageMode.USER));
        final var embed = embedArgumentCaptor.getValue();
        assertThat(embed.getFields()).hasSize(2);
        assertThat(embed.getFields().get(0).getName()).isEqualTo(eventTwo.getTitle());
        assertThat(embed.getFields().get(0).getValue()).isEqualTo(summary(eventTwo, config));
        assertThat(embed.getFields().get(1).getName()).isEqualTo(eventThree.getTitle());
        assertThat(embed.getFields().get(1).getValue()).isEqualTo(summary(eventThree, config));
    }

    @SuppressWarnings("unchecked")
    @Test
    void filterViewEvents_noUpcomingSpecified() {
        final var config = TestEntities.configJpa();
        configRepository.saveAndFlush(config);
        final var courseOne = TestEntities.courseJpa(1);
        final var courseTwo = TestEntities.courseJpa(2);
        final var courseThree = TestEntities.courseJpa(3);
        courseRepository.saveAllAndFlush(List.of(courseOne, courseTwo, courseThree));
        final var student = TestEntities.studentJpa(1, List.of(courseOne, courseTwo));
        studentRepository.saveAndFlush(student);

        final var currentDate = LocalDateTime.now();
        // before min
        final var eventOne = createEvent(1, courseOne);
        eventOne.setEventDate(currentDate.minusDays(1));
        // after min and before max
        final var eventTwo = createEvent(2, courseOne);
        eventTwo.setEventDate(currentDate.plusDays(1));
        final var eventThree = createEvent(3, courseOne);
        eventThree.setEventDate(currentDate.plusDays(2));
        final var eventFour = createEvent(4, courseOne);
        eventFour.setEventDate(currentDate.plusDays(3));
        // after min and max
        final var eventFive = createEvent(5, courseOne);
        eventFive.setEventDate(currentDate.plusDays(20));
        // doesn't match course
        final var eventSix = createEvent(6, courseTwo);
        eventSix.setEventDate(currentDate.plusDays(1));
        final var events = List.of(eventOne, eventTwo, eventThree, eventFour, eventFive, eventSix);
        eventRepository.saveAllAndFlush(events);
        entityManager.clear();

        final var request = mock(CommandRequest.class);
        final var requester = TestEntities.member();
        final Map<String, String> arguments = new HashMap<>();
        arguments.put(TIME_PERIOD, "15");
        arguments.put(COURSE, courseOne.getCode());
        final var jdaEvent = mock(SlashCommandInteractionEvent.class);
        final var channel = mock(TextChannel.class);
        final RestAction<Message> restAction = mock(RestAction.class);
        final var message = mock(Message.class);
        final var future = CompletableFuture.completedFuture(message);

        when(request.getRequester())
            .thenReturn(requester);
        when(requester.getIdLong())
            .thenReturn(student.getId());
        when(request.getArguments())
            .thenReturn(arguments);

        when(request.getEvent())
            .thenReturn(jdaEvent);
        when(jdaEvent.getJDA())
            .thenReturn(jda);
        when(jda.getTextChannelById(config.getEventChannel()))
            .thenReturn(channel);
        when(channel.retrieveMessageById(anyLong()))
            .thenReturn(restAction);
        when(restAction.submit())
            .thenReturn(future);

        eventService.filterViewEvents(request);

        verify(request).deferReply(MessageMode.USER);
        verify(request, never()).sendResponse(anyString(), any());
        final ArgumentCaptor<MessageEmbed> embedArgumentCaptor = ArgumentCaptor.forClass(MessageEmbed.class);
        verify(request).sendResponse(embedArgumentCaptor.capture(), eq(MessageMode.USER));
        final var embed = embedArgumentCaptor.getValue();

        assertThat(embed.getFields()).hasSize(3);
        assertThat(embed.getFields().get(0).getName()).isEqualTo(eventTwo.getTitle());
        assertThat(embed.getFields().get(0).getValue()).isEqualTo(summary(eventTwo, config));
        assertThat(embed.getFields().get(1).getName()).isEqualTo(eventThree.getTitle());
        assertThat(embed.getFields().get(1).getValue()).isEqualTo(summary(eventThree, config));
        assertThat(embed.getFields().get(2).getName()).isEqualTo(eventFour.getTitle());
        assertThat(embed.getFields().get(2).getValue()).isEqualTo(summary(eventFour, config));
    }

    @SuppressWarnings("unchecked")
    @Test
    void filterViewEvents_noCourseSpecified() {
        final var config = TestEntities.configJpa();
        configRepository.saveAndFlush(config);
        final var courseOne = TestEntities.courseJpa(1);
        final var courseTwo = TestEntities.courseJpa(2);
        final var courseThree = TestEntities.courseJpa(3);
        courseRepository.saveAllAndFlush(List.of(courseOne, courseTwo, courseThree));
        final var student = TestEntities.studentJpa(1, List.of(courseOne, courseTwo));
        studentRepository.saveAndFlush(student);

        final var currentDate = LocalDateTime.now();
        // before min
        final var eventOne = createEvent(1, courseOne);
        eventOne.setEventDate(currentDate.minusDays(1));
        // after min and before max
        final var eventTwo = createEvent(2, courseOne);
        eventTwo.setEventDate(currentDate.plusDays(1));
        final var eventThree = createEvent(3, courseOne);
        eventThree.setEventDate(currentDate.plusDays(20));
        // doesn't match course
        final var eventFour = createEvent(4, courseTwo);
        eventFour.setEventDate(currentDate.plusDays(3));
        final var events = List.of(eventOne, eventTwo, eventThree, eventFour);
        eventRepository.saveAllAndFlush(events);
        entityManager.clear();

        final var request = mock(CommandRequest.class);
        final var requester = TestEntities.member();
        final Map<String, String> arguments = new HashMap<>();
        arguments.put(TIME_PERIOD, "15");
        arguments.put(UPCOMING, "2");
        final var jdaEvent = mock(SlashCommandInteractionEvent.class);
        final var channel = mock(TextChannel.class);
        final RestAction<Message> restAction = mock(RestAction.class);
        final var message = mock(Message.class);
        final var future = CompletableFuture.completedFuture(message);

        when(request.getRequester())
            .thenReturn(requester);
        when(requester.getIdLong())
            .thenReturn(student.getId());
        when(request.getArguments())
            .thenReturn(arguments);

        when(request.getEvent())
            .thenReturn(jdaEvent);
        when(jdaEvent.getJDA())
            .thenReturn(jda);
        when(jda.getTextChannelById(config.getEventChannel()))
            .thenReturn(channel);
        when(channel.retrieveMessageById(anyLong()))
            .thenReturn(restAction);
        when(restAction.submit())
            .thenReturn(future);

        eventService.filterViewEvents(request);

        verify(request).deferReply(MessageMode.USER);
        verify(request, never()).sendResponse(anyString(), any());
        final ArgumentCaptor<MessageEmbed> embedArgumentCaptor = ArgumentCaptor.forClass(MessageEmbed.class);
        verify(request).sendResponse(embedArgumentCaptor.capture(), eq(MessageMode.USER));
        final var embed = embedArgumentCaptor.getValue();

        assertThat(embed.getFields()).hasSize(2);
        assertThat(embed.getFields().get(0).getName()).isEqualTo(eventTwo.getTitle());
        assertThat(embed.getFields().get(0).getValue()).isEqualTo(summary(eventTwo, config));
        assertThat(embed.getFields().get(1).getName()).isEqualTo(eventFour.getTitle());
        assertThat(embed.getFields().get(1).getValue()).isEqualTo(summary(eventFour, config));
    }

    private String summary(EventJpa event, ConfigJpa config) {
        return """
            %s
            %s
            %s
            %s""".formatted(
            MarkdownUtil.bold("Date"),
            DateFormatter.formattedDateTimeWithTimestamp(event.getEventDate()),
            MarkdownUtil.bold("Link"),
            "https://discord.com/channels/%d/%d/%d".formatted(config.getServerId(), config.getEventChannel(), event.getMessageId())
        );
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
            .status(EventStatus.ACTIVE)
            .build();
    }
}
