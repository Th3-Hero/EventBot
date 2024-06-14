package com.th3hero.eventbot.services;

import com.th3hero.eventbot.TestEntities;
import com.th3hero.eventbot.commands.requests.CommandRequest;
import com.th3hero.eventbot.commands.requests.InteractionRequest;
import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.repositories.EventDraftRepository;
import com.th3hero.eventbot.repositories.EventRepository;
import jakarta.persistence.EntityManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.RestAction;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.th3hero.eventbot.TestEntities.TEST_DATE;
import static com.th3hero.eventbot.utils.DiscordFieldsUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EventServiceIT {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventService eventService;

    @MockBean
    private CourseService courseService;
    @MockBean
    private ConfigService configService;
    @MockBean
    private EventDraftRepository eventDraftRepository;
    @MockBean
    private SchedulingService schedulingService;
    @MockBean
    private StudentService studentService;

//    @SuppressWarnings("unchecked")
//    @Test
//    void filterViewEvents() {
//        final var courseOne = TestEntities.courseJpa(1);
//        final var courseTwo = TestEntities.courseJpa(2);
//        final var minDate = LocalDateTime.of(2025, 5, 5, 12, 0);
//        // before min
//        final var eventOne = createEvent(1, courseOne);
//        eventOne.setEventDate(minDate.minusDays(1));
//        // after min and before max
//        final var eventTwo = createEvent(2, courseOne);
//        eventTwo.setEventDate(minDate.plusDays(1));
//        final var eventThree = createEvent(3, courseOne);
//        eventThree.setEventDate(minDate.plusDays(2));
//        final var eventFour = createEvent(4, courseOne);
//        eventFour.setEventDate(minDate.plusDays(3));
//        final var eventFive = createEvent(5, courseOne);
//        eventFive.setEventDate(minDate.plusDays(4));
//        // after min and max
//        final var eventSix = createEvent(6, courseOne);
//        eventSix.setEventDate(minDate.plusDays(20));
//        // doesn't match course
//        final var eventSeven = createEvent(7, courseTwo);
//        eventSeven.setEventDate(minDate.plusDays(1));
//        final var events = List.of(eventOne, eventTwo, eventThree, eventFour, eventSix, eventSeven);
//        eventRepository.saveAllAndFlush(events);
//        entityManager.clear();
//
//        final var request = mock(CommandRequest.class);
//        final var requester = TestEntities.member();
//        final Map<String, String> arguments = new HashMap<>();
//        arguments.put(TIME_PERIOD, "15");
//        arguments.put(UPCOMING, "2");
//        arguments.put(COURSE, courseOne.getCode());
//        final var config = TestEntities.configJpa();
//        final var jdaEvent = mock(SlashCommandInteractionEvent.class);
//        final var jda = mock(JDA.class);
//        final var channel = mock(TextChannel.class);
//        final RestAction<Message> restAction = mock(RestAction.class);
//        final var message = mock(Message.class);
//        final var future = CompletableFuture.completedFuture(message);
//
//        final var student = TestEntities.studentJpa(1, List.of(courseOne, courseTwo));
//        try (final MockedStatic<LocalDateTime> localDateTime = mockStatic(LocalDateTime.class)) {
//            when(request.getRequester())
//                .thenReturn(requester);
//            when(studentService.fetchStudent(requester.getIdLong()))
//                .thenReturn(student);
//            when(request.getArguments())
//                .thenReturn(arguments);
//            when(courseService.coursesFromCourseCodes(List.of(courseOne.getCode())))
//                .thenReturn(List.of(courseOne));
//            localDateTime.when(LocalDateTime::now)
//                .thenReturn(minDate);
//            when(configService.getConfigJpa())
//                .thenReturn(config);
//            when(request.getEvent())
//                .thenReturn(jdaEvent);
//            when(jdaEvent.getJDA())
//                .thenReturn(jda);
//            when(jda.getTextChannelById(config.getEventChannel()))
//                .thenReturn(channel);
//            when(channel.retrieveMessageById(anyLong()))
//                .thenReturn(restAction);
//            when(restAction.submit())
//                .thenReturn(future);
//
//            eventService.filterViewEvents(request);
//
//            verify(request).deferReply(InteractionRequest.MessageMode.USER);
//            verify(request, never()).sendResponse(anyString(), eq(InteractionRequest.MessageMode.USER));
//        }
//    }
//
//    @Test
//    void filterViewEvents_noTimePeriodSpecified() {
//        // NOTE: I really don't know how to test these at this point
//        assertThat(true).isFalse();
//    }
//
//    @Test
//    void filterViewEvents_noUpcomingSpecified() {
//
//        assertThat(true).isFalse();
//    }
//
//    @Test
//    void filterViewEvents_noCourseSpecified() {
//
//        assertThat(true).isFalse();
//    }

//    private List<EventJpa> filterTestList(CourseJpa course, LocalDateTime minTime) {
//        // before min
//        final var eventOne = createEvent(1, course);
//        eventOne.setEventDate(minTime.minusDays(1));
//        // after min and before max
//        final var eventTwo = createEvent(2, course);
//        eventTwo.setEventDate(minTime.plusDays(1));
//        // after min and max
//        final var eventThree = createEvent(5, course);
//        eventThree.setEventDate(minTime.plusDays(20));
//        return List.of(eventOne, eventTwo, eventThree);
//    }

    private EventJpa createEvent(int seed, CourseJpa course) {
        return EventJpa.builder()
            .id(1234L + seed)
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
