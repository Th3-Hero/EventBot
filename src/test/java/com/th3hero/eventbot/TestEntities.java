package com.th3hero.eventbot;

import com.th3hero.eventbot.entities.ConfigJpa;
import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.entities.StudentJpa;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
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

    public static EventJpa createEventJpa() {
        return EventJpa.builder()
            .id(1234L)
            .title("Test Event")
            .note("Test Note")
            .authorId(1234L)
            .datetime(LocalDateTime.now())
            .type(EventJpa.EventType.ASSIGNMENT)
            .build();
    }

    public static ConfigJpa createConfigJpa() {
        return ConfigJpa.builder()
            .id(1234)
            .eventChannel(1234L)
            .build();
    }

    public static CourseJpa courseJpa(int seed) {
        return CourseJpa.builder()
            .code("TEST%s".formatted(seed))
            .name("Test Course%s".formatted(seed))
            .nickname("Test%s".formatted(seed))
            .build();
    }

    public static EventJpa eventJpa(int seed, List<CourseJpa> courses) {
        return EventJpa.builder()
            .authorId(1234L + seed)
            .messageId(1234L + seed)
            .title("Test Event%s".formatted(seed))
            .note("Test Note%s".formatted(seed))
            .datetime(LocalDateTime.of(2025, 1, 1, 1, 1, 1))
            .type(EventJpa.EventType.ASSIGNMENT)
            .courses(new ArrayList<>(courses))
            .build();
    }

    public static StudentJpa studentJpa(int seed, List<CourseJpa> courses) {
        return StudentJpa.builder()
            .id(1234L + seed)
            .offsetTimes(List.of(24, 72))
            .courses(new ArrayList<>(courses))
            .build();
    }

    public static Guild guild() {
        return guild(1);
    }

    // Ignored for mocking reasons. I don't actually care what value it takes on, so long as it does not NPE
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
}
