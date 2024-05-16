package com.th3hero.eventbot.factories;

import com.th3hero.eventbot.commands.actions.Command;
import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.entities.EventDraftJpa;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.exceptions.DiscordConstraintException;
import com.th3hero.eventbot.formatting.DateFormatting;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class EmbedBuilderFactory {
    public static int MAX_EMBED_FIELDS = 25;

    private static final Color BLUE = new Color(3, 123, 252);
    private static final Color GREEN = new Color(0, 255, 0);
    private static final Color RED = new Color(255, 8, 0);


    /**
     * @return A help embed with all the commands and their descriptions.
     */
    public static MessageEmbed help() {
        EmbedBuilder embedBuilder = new EmbedBuilder()
            .setColor(BLUE)
            .setTitle("Help:");

        for (Command command : Command.values()) {
            embedBuilder.addField(
                command.name().toLowerCase(),
                Command.DESCRIPTIONS.get(command.getDisplayName()),
                false
            );
        }

        return embedBuilder.build();
    }

    public static MessageEmbed coursePicker(String description) {
        return coursePicker("Course Selection", description);
    }

    public static MessageEmbed coursePicker(String title, String description) {
        return new EmbedBuilder()
            .setColor(BLUE)
            .setTitle(title)
            .setDescription(description)
            .build();
    }

    public static MessageEmbed selectedCourses(List<CourseJpa> courses) {
        if (courses.size() > MAX_EMBED_FIELDS) {
            throw new DiscordConstraintException("Too many courses selected. Discord limits to %d fields.".formatted(MAX_EMBED_FIELDS));
        }
        EmbedBuilder embedBuilder = new EmbedBuilder()
            .setColor(BLUE)
            .setTitle("Your Courses");

        for (CourseJpa course : courses) {
            embedBuilder.addField(
                course.getCode(),
                course.getName(),
                false
            );
        }

        return embedBuilder.build();
    }

    private static MessageEmbed draftLayout(String title, String note, String type, String date, List<CourseJpa> courses, String authorMention) {
        return new EmbedBuilder()
            .setColor(GREEN)
            .setTitle(title)
            .setDescription(note)
            .addField(
                "Date",
                date,
                false
            )
            .addField(
                "Type",
                type,
                false
            )
            .addField(
                "Course(s)",
                courses.stream().map(CourseJpa::getCode).collect(Collectors.joining("\n")),
                false
            )
            .addField(
                "Author",
                authorMention,
                false
            )
            .build();
    }

    public static List<MessageEmbed> displayEventDraft(EventDraftJpa eventDraftJpa, int draftCleanupDelay, String authorMention) {
        MessageEmbed header = new EmbedBuilder()
            .setColor(BLUE)
            .setTitle("Event Draft Preview")
            .setDescription("Preview the draft and make any edits before confirming.")
            .setFooter("Note: Drafts that are not confirmed within %d hours will be deleted.".formatted(draftCleanupDelay))
            .build();

        String date = DateFormatting.formattedDateTimeWithTimestamp(eventDraftJpa.getDatetime());

        MessageEmbed eventDraft = draftLayout(
            eventDraftJpa.getTitle(),
            eventDraftJpa.getNote(),
            eventDraftJpa.getType().displayName(),
            date,
            eventDraftJpa.getCourses(),
            authorMention
        );

        return List.of(header, eventDraft);
    }

    public static MessageEmbed reminderOffsets(List<Integer> offsets) {
        return new EmbedBuilder()
            .setColor(BLUE)
            .setTitle("Your reminder offsets")
            .setDescription("Reminder offsets are how many hours prior to an event you wish to be notified. You can have multiple offsets.")
            .addField(
                "Your offsets:",
                offsets.stream().map(Long::toString).collect(Collectors.joining("\n")),
                true
            )
            .build();
    }

    public static MessageEmbed eventEmbed(EventJpa eventJpa, String authorMention) {
        String date = DateFormatting.formattedDateTimeWithTimestamp(eventJpa.getDatetime());

        return draftLayout(
            eventJpa.getTitle(),
            eventJpa.getNote(),
            eventJpa.getType().displayName(),
            date,
            eventJpa.getCourses(),
            authorMention
        );
    }

    public static MessageEmbed deleteEvent(String reason, String jumpUrl, String mention, int eventCleanupDelay) {
        return new EmbedBuilder()
            .setTitle("Event Deleted")
            .setDescription("The following event has been deleted: %s".formatted(jumpUrl))
            .addField(
                "This action has been taken by:",
                mention,
                false
            )
            .addField(
                "Reason",
                reason,
                false
            )
            .addField(
                "Information",
                "- Once an event has been deleted no reminders will be sent for the event.%n- The event can be recovered within the next %d hours using the undo button.%n- After %d hours, the event will be entirely cleaned up.".formatted(eventCleanupDelay, eventCleanupDelay),
                false
            )
            .setColor(BLUE)
            .build();
    }

    public static MessageEmbed eventRestored(String mention) {
        return new EmbedBuilder()
            .setTitle("Event Restored")
            .setDescription("The event has been restored. Reminders will be sent out as scheduled.")
            .addField(
                "The event was restored by:",
                mention,
                false
            )
            .setColor(BLUE)
            .build();
    }

    public static EmbedBuilder eventEditsStarter() {
        return new EmbedBuilder()
            .setColor(BLUE)
            .setTitle("Event has been edited.")
            .setDescription("Find the changes below.");
    }

    private static String shortEventSummary(EventJpa eventJpa, String jumpUrl) {
        String date = DateFormatting.formattedDateTimeWithTimestamp(eventJpa.getDatetime());

        return "%s%s%s%s".formatted(
            "**Date**\n",
            "%s%n".formatted(date),
            "**Link**\n",
            jumpUrl
        );
    }

    public static MessageEmbed eventList(Map<EventJpa, String> events) {
        EmbedBuilder embedBuilder = new EmbedBuilder()
            .setColor(BLUE)
            .setTitle("Upcoming Events");

        for (Map.Entry<EventJpa, String> entry : events.entrySet()) {
            embedBuilder.addField(
                entry.getKey().getTitle(),
                shortEventSummary(entry.getKey(), entry.getValue()),
                false
            );
        }

        return embedBuilder.build();
    }
}
