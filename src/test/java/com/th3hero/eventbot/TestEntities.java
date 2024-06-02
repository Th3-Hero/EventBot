package com.th3hero.eventbot;

import com.th3hero.eventbot.dto.config.ConfigUpload;
import com.th3hero.eventbot.dto.course.CourseUpdate;
import com.th3hero.eventbot.dto.course.CourseUpload;
import com.th3hero.eventbot.entities.*;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.Component;
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

    // DTOs
    public static ConfigUpload configUpload() {
        return new ConfigUpload(1234L, 48, 24);
    }
    public static ConfigUpload configUpload_nullDefaults() {
        return new ConfigUpload(1234L, null, null);
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

    public static EventDraftJpa draftMissingDetailsAndCourses() {
        return EventDraftJpa.builder()
            .authorId(1234L)
            .eventDate(LocalDateTime.now())
            .type(EventJpa.EventType.ASSIGNMENT)
            .build();
    }

    public static EventDraftJpa eventDraftJpa(int seed) {
        return EventDraftJpa.builder()
            .id(1234L + seed)
            .authorId(1234L + seed)
            .title("Test Event%s".formatted(seed))
            .note("Test Note%s".formatted(seed))
            .eventDate(LocalDateTime.of(2099, 1, 1, 1, 1, 1))
            .draftCreationDate(LocalDateTime.of(2099, 1, 1, 1, 1, 1))
            .type(EventJpa.EventType.ASSIGNMENT)
            .courses(new ArrayList<>(List.of()))
            .build();
    }

    public static EventJpa eventJpa(int seed) {
        return eventJpa(seed, List.of());
    }

    public static EventJpa eventJpaWithId(int seed) {
        return EventJpa.builder()
            .id(1234L + seed)
            .authorId(1234L + seed)
            .messageId(1234L + seed)
            .title("Test Event%s".formatted(seed))
            .note("Test Note%s".formatted(seed))
            .eventDate(LocalDateTime.of(2099, 1, 1, 1, 1, 1))
            .type(EventJpa.EventType.ASSIGNMENT)
            .courses(new ArrayList<>(List.of()))
            .build();
    }

    public static EventJpa eventJpa(int seed, List<CourseJpa> courses) {
        return EventJpa.builder()
//            .id(1234L + seed)
            .authorId(1234L + seed)
            .messageId(1234L + seed)
            .title("Test Event%s".formatted(seed))
            .note("Test Note%s".formatted(seed))
            .eventDate(LocalDateTime.of(2099, 1, 1, 1, 1, 1))
            .type(EventJpa.EventType.ASSIGNMENT)
            .courses(new ArrayList<>(courses))
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

    public static Member member(final Guild guild) {
        return new MemberImpl((GuildImpl) guild, user());
    }

    public static Message message() {
        return mock(Message.class);
    }

    public static ModalMapping modalMapping(String key, Object value) {
        final DataObject data = DataObject.empty();
        data.put("custom_id", key);
        data.put("type", Component.Type.TEXT_INPUT.getKey());
        data.put("value", value);
        return new ModalMapping(data);
    }
}
