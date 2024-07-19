package com.th3hero.eventbot;

import com.th3hero.eventbot.dto.config.ConfigUpload;
import com.th3hero.eventbot.dto.course.CourseUpdate;
import com.th3hero.eventbot.dto.course.CourseUpload;
import com.th3hero.eventbot.entities.ConfigJpa;
import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.entities.EventJpa.EventStatus;
import com.th3hero.eventbot.entities.StudentJpa;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.Component;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.GuildImpl;
import net.dv8tion.jda.internal.entities.MemberImpl;
import net.dv8tion.jda.internal.entities.UserImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.mockito.Mockito.*;

public class TestEntities {

    public static final LocalDateTime TEST_DATE = LocalDateTime.of(2099, 1, 1, 1, 1);

    // DTOs
    public static ConfigUpload configUpload() {
        return new ConfigUpload(1234L, 69L, 48, 24);
    }
    public static ConfigUpload configUpload_nullDefaults() {
        return new ConfigUpload(1234L, 4321L, null, null);
    }

    public static CourseUpload courseUpload(int seed) {
        return new CourseUpload("TEST%s".formatted(seed), "Test Course%s".formatted(seed));
    }

    // Entities
    public static EventJpa createEventJpa() {
        return EventJpa.builder()
            .id(1234L)
            .title("Test Event")
            .note("Test Note")
            .authorId(1234L)
            .eventDate(LocalDateTime.now())
            .type(EventJpa.EventType.ASSIGNMENT)
            .build();
    }

    public static ConfigJpa configJpa() {
        return ConfigJpa.builder()
            .id(1234)
            .serverId(5678L)
            .eventChannel(4321L)
            .build();
    }

    public static CourseJpa courseJpa(int seed) {
        return CourseJpa.builder()
            .code("TEST%s".formatted(seed))
            .name("Test Course%s".formatted(seed))
            .build();
    }

    public static CourseUpdate courseUpdate(int seed) {
        return new CourseUpdate("TEST%s".formatted(seed), "Test Course%s".formatted(seed));
    }

    public static EventJpa draftMissingDetailsAndCourses() {
        return EventJpa.builder()
            .authorId(1234L)
            .eventDate(LocalDateTime.now())
            .type(EventJpa.EventType.ASSIGNMENT)
            .status(EventStatus.DRAFT)
            .build();
    }

    public static EventJpa eventDraft(int seed) {
        return EventJpa.builder()
            .id(1234L + seed)
            .authorId(1234L + seed)
            .title("Test Event%s".formatted(seed))
            .note("Test Note%s".formatted(seed))
            .eventDate(TEST_DATE)
            .creationDate(TEST_DATE)
            .type(EventJpa.EventType.ASSIGNMENT)
            .courses(new ArrayList<>(List.of()))
            .status(EventStatus.DRAFT)
            .build();
    }

    public static EventJpa eventJpa(int seed) {
        return eventJpa(seed, List.of());
    }

    public static EventJpa eventJpaWithId(int seed) {
        final var courses = List.of(
            TestEntities.courseJpa(seed+1),
            TestEntities.courseJpa(seed+2),
            TestEntities.courseJpa(seed+3)
        );
        for (int i = 0; i < courses.size(); i++) {
            for (int j = 0; j < courses.size(); j++) {
                courses.get(i).getStudents().add(TestEntities.studentJpa(j, List.of()));
            }
        }

        return EventJpa.builder()
            .id(1234L + seed)
            .authorId(1234L + seed)
            .messageId(1234L + seed)
            .title("Test Event%s".formatted(seed))
            .note("Test Note%s".formatted(seed))
            .eventDate(TEST_DATE)
            .type(EventJpa.EventType.ASSIGNMENT)
            .courses(new ArrayList<>(courses))
            .status(EventStatus.ACTIVE)
            .build();
    }

    public static EventJpa eventJpa(int seed, List<CourseJpa> courses) {
        return EventJpa.builder()
            .authorId(1234L + seed)
            .messageId(1234L + seed)
            .title("Test Event%s".formatted(seed))
            .note("Test Note%s".formatted(seed))
            .eventDate(TEST_DATE)
            .type(EventJpa.EventType.ASSIGNMENT)
            .courses(new ArrayList<>(courses))
            .status(EventStatus.ACTIVE)
            .build();
    }

    public static StudentJpa studentJpa(int seed, List<CourseJpa> courses) {
        return StudentJpa.builder()
            .id(1234L + seed)
            .reminderOffsetTimes(new ArrayList<>(List.of(24, 72)))
            .courses(new ArrayList<>(courses))
            .build();
    }

    public static Guild guild() {
        return guild(1);
    }

    public static Guild guild(final int id) {
        final JDAImpl jda = mock(JDAImpl.class);

        when(jda.getCacheFlags())
            .thenReturn(EnumSet.noneOf(CacheFlag.class));

        return spy(new GuildImpl(jda, id));
    }

    private static User user() {
        return new UserImpl(1, mock(JDAImpl.class)).setName("user").setGlobalName("test_user").setBot(false);
    }

    public static Member member() {
        return spy(new MemberImpl((GuildImpl)guild(), user()));
    }

    public static Message message() {
        return mock(Message.class);
    }

    public static ModalMapping modalMapping(String key, Object value) {
        final DataObject data = DataObject.empty();
        data.put("custom_id", key);
        data.put("type", Component.Type.TEXT_INPUT.getKey());
        data.put("value", value);
        return spy(new ModalMapping(data));
    }

    public static StringSelectMenu courseSelectMenu() {
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
