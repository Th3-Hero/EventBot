package com.th3hero.eventbot.utils;

import com.th3hero.eventbot.commands.Command;
import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.entities.EventDraftJpa;
import com.th3hero.eventbot.entities.EventJpa;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class EmbedBuilderFactory {
    private static final Color BLUE = new Color(3, 123, 252);
    private static final Color GREEN = new Color(0, 255, 0);
    private static final Color RED = new Color(255, 8, 0);

    public static MessageEmbed help() {
        EmbedBuilder embedBuilder =  new EmbedBuilder()
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

        String date = "%s (%s)".formatted(
                Utils.formattedDateTime(eventDraftJpa.getDatetime()),
                DiscordTimestamp.create(DiscordTimestamp.RELATIVE, eventDraftJpa.getDatetime())
        );

        MessageEmbed eventDraft = new EmbedBuilder()
                .setColor(RED)
                .setTitle(eventDraftJpa.getTitle())
                .setDescription(eventDraftJpa.getNote())
                .addField(
                        "Date",
                        date,
                        false
                )
                .addField(
                        "Type",
                        eventDraftJpa.getType().displayName(),
                        false
                )
                .addField(
                        "Course(s)",
                        eventDraftJpa.getCourses().stream().map(CourseJpa::getCode).collect(Collectors.joining("\n")),
                        false
                )
                .addField(
                        "Author",
                        authorMention,
                        false
                )
                .build();

        return List.of(header, eventDraft);
    }

    public static MessageEmbed editDraftMenu() {
        return new EmbedBuilder()
                .setColor(BLUE)
                .setTitle("Edit Draft")
                .setDescription("Select what about the draft you would like to edit.")
                .build();
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
        String date = "%s (%s)".formatted(
                Utils.formattedDateTime(eventJpa.getDatetime()),
                DiscordTimestamp.create(DiscordTimestamp.RELATIVE, eventJpa.getDatetime())
        );

        return new EmbedBuilder()
                .setColor(GREEN)
                .setTitle(eventJpa.getTitle())
                .setDescription(eventJpa.getNote())
                .addField(
                        "Date",
                        date,
                        false
                )
                .addField(
                        "Type",
                        eventJpa.getType().displayName(),
                        false
                )
                .addField(
                        "Course(s)",
                        eventJpa.getCourses().stream().map(CourseJpa::getCode).collect(Collectors.joining("\n")),
                        false
                )
                .addField(
                        "Author",
                        authorMention,
                        false
                )
                .build();
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
}
