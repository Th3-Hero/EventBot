package com.th3hero.eventbot.factories;

import com.th3hero.eventbot.commands.actions.Command;
import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.entities.EventDraftJpa;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.exceptions.IllegalInteractionException;
import com.th3hero.eventbot.formatting.DateFormatter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.NONE)
public final class EmbedBuilderFactory {

    private static final Color BLUE = new Color(3, 123, 252);
    private static final Color GREEN = new Color(0, 255, 0);
    private static final Color RED = new Color(255, 8, 0);


    /**
     * @return A help embed with all the commands and their descriptions.
     */
    public static MessageEmbed help() {
        EmbedBuilder embedBuilder = new EmbedBuilder()
            .setColor(BLUE)
            .setTitle("Help")
            .setDescription("NOTE: In order to receive notification from the bot, the %s".formatted(MarkdownUtil.bold("bot must be able to send you direct messages.")))
            .setFooter("Disclaimer: This bot is a tool provided 'as is'. The developer/maintainer is not responsible for any missed events or any other issues that may arise from the use of this bot. Use at your own risk.");

        for (Command command : Command.values()) {
            embedBuilder.addField(
                command.name().toLowerCase(),
                Command.DESCRIPTIONS.get(command.getDisplayName()),
                false
            );
        }

        return embedBuilder.build();
    }

    public static MessageEmbed courseSelectionHeader(String description) {
        return new EmbedBuilder()
            .setColor(BLUE)
            .setTitle("Course Selection")
            .setDescription(description)
            .build();
    }


    /**
     * @param courses The courses to display in the embed
     * @return An embed displaying the selected courses
     */
    public static MessageEmbed selectedCourses(List<CourseJpa> courses) {
        if (courses.size() > MessageEmbed.MAX_FIELD_AMOUNT) {
            throw new IllegalInteractionException("Too many courses selected. Discord limits to %d fields.".formatted(MessageEmbed.MAX_FIELD_AMOUNT));
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

    public static List<MessageEmbed> displayEventDraft(EventDraftJpa eventDraftJpa, int draftCleanupDelay, String authorMention) {
        MessageEmbed header = new EmbedBuilder()
            .setColor(BLUE)
            .setTitle("Event Draft Preview")
            .setDescription("Preview the draft and make any edits before confirming.")
            .setFooter("Note: Drafts that are not confirmed within %d hours will be deleted.".formatted(draftCleanupDelay))
            .build();

        String date = DateFormatter.formattedDateTimeWithTimestamp(eventDraftJpa.getEventDate());

        MessageEmbed eventDraft = eventLayout(
            eventDraftJpa.getTitle(),
            eventDraftJpa.getNote(),
            date,
            eventDraftJpa.getType().displayName(),
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
        String date = DateFormatter.formattedDateTimeWithTimestamp(eventJpa.getEventDate());

        return eventLayout(
            eventJpa.getTitle(),
            eventJpa.getNote(),
            date,
            eventJpa.getType().displayName(),
            eventJpa.getCourses(),
            authorMention
        );
    }

    public static MessageEmbed deletedEvent(String reason, String jumpUrl, String mention, int eventCleanupDelay) {
        return new EmbedBuilder()
            .setColor(BLUE)
            .setTitle("Event Deleted")
            .setDescription("The following event has been deleted: %s".formatted(jumpUrl))
            .addField(
                "Deleted by",
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
                """
                    - Once an event has been deleted no reminders will be sent for the event.
                    - The event can be recovered within the next %d hours using the undo button. Afterwards it will be deleted. \
                    """.formatted(eventCleanupDelay),
                false
            )
            .build();
    }

    public static MessageEmbed eventRestored(String mention) {
        return new EmbedBuilder()
            .setColor(BLUE)
            .setTitle("Event Restored")
            .setDescription("The event has been restored. Reminders will be sent out as scheduled.")
            .addField(
                "Restored by",
                mention,
                false
            )
            .build();
    }

    public static MessageEmbed editedEventDetailsChangelog(String title, EventJpa eventJpa, String note, LocalDateTime eventDate) {
        EmbedBuilder embedBuilder = editedEventChangelogStarted();

        if (!title.equals(eventJpa.getTitle())) {
            embedBuilder.addField(
                "Original Title", eventJpa.getTitle(), false
            ).addField(
                "Updated Title", title, false
            );
            eventJpa.setTitle(title);
        }
        if (!StringUtils.equals(note, eventJpa.getNote())) {
            if (StringUtils.isBlank(eventJpa.getNote()) && !StringUtils.isBlank(note)) {
                embedBuilder.addField(
                    "Added Note", note, false
                );
            } else {
                embedBuilder.addField(
                    "Original Note", eventJpa.getNote(), false
                ).addField(
                    "Updated Note", StringUtils.isBlank(note) ? "*Note was removed*" : note, false
                );
            }
            eventJpa.setNote(StringUtils.isBlank(note) ? null : note);
        }
        if (!eventDate.equals(eventJpa.getEventDate())) {
            embedBuilder.addField(
                "Original Date",
                DateFormatter.formattedDateTime(eventJpa.getEventDate()),
                false
            ).addField(
                "Updated Date",
                DateFormatter.formattedDateTime(eventDate),
                false
            );
        }

        return embedBuilder.build();
    }

    public static MessageEmbed editedEventCoursesChangelog(EventJpa eventJpa, List<CourseJpa> selectedCourses) {
        EmbedBuilder embedBuilder = editedEventChangelogStarted();

        embedBuilder.addField(
            "Original Courses",
            eventJpa.getCourses().stream().map(CourseJpa::getCode).collect(Collectors.joining("\n")),
            false
        ).addField(
            "Updated Courses",
            selectedCourses.stream().map(CourseJpa::getCode).collect(Collectors.joining("\n")),
            false
        );

        return embedBuilder.build();
    }

    public static MessageEmbed listEvents(Map<EventJpa, String> events) {
        if (events.size() > MessageEmbed.MAX_FIELD_AMOUNT) {
            throw new IllegalInteractionException("Too many events. Discord limits to %d fields.".formatted(MessageEmbed.MAX_FIELD_AMOUNT));
        }
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

    private static MessageEmbed eventLayout(String title, String note, String date, String type, List<CourseJpa> courses, String authorMention) {
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

    private static String shortEventSummary(EventJpa eventJpa, String jumpUrl) {
        String date = DateFormatter.formattedDateTimeWithTimestamp(eventJpa.getEventDate());

        return
            """
                %s
                %s
                %s
                %s
                """.formatted(MarkdownUtil.bold("Date"), date, MarkdownUtil.bold("Link"), jumpUrl);
    }

    private static EmbedBuilder editedEventChangelogStarted() {
        return new EmbedBuilder()
            .setColor(BLUE)
            .setTitle("Event has been edited.")
            .setDescription("Find the changes below.");
    }
}
