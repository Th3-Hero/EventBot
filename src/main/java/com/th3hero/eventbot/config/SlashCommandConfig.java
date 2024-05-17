package com.th3hero.eventbot.config;

import com.th3hero.eventbot.commands.actions.Command;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.services.StudentService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static com.th3hero.eventbot.utils.DiscordFieldsUtils.*;

@Component
@RequiredArgsConstructor
public class SlashCommandConfig {
    private final JDA jda;

    @EventListener(ApplicationReadyEvent.class)
    public void registerSlashCommands() {
        jda.updateCommands().addCommands(
            Commands.slash(Command.HELP.getDisplayName(), Command.DESCRIPTIONS.get(Command.HELP.getDisplayName())),
            Commands.slash(Command.SELECT_COURSES.getDisplayName(), Command.DESCRIPTIONS.get(Command.SELECT_COURSES.getDisplayName())),
            Commands.slash(Command.MY_COURSES.getDisplayName(), Command.DESCRIPTIONS.get(Command.MY_COURSES.getDisplayName())),
            Commands.slash(Command.CREATE_EVENT.getDisplayName(), Command.DESCRIPTIONS.get(Command.CREATE_EVENT.getDisplayName())).addOptions(
                new OptionData(OptionType.STRING, DATE_ID, "Date of the event in order of year, month, day. Eg. 2025-4-24 or 2025/4/24.", true)
                    .setRequiredLength(MIN_DATE_LENGTH, MAX_DATE_LENGTH),
                new OptionData(OptionType.STRING, TIME_ID, "Time of the event in 24 hour time. Eg. 14:30", true)
                    .setRequiredLength(MIN_TIME_LENGTH, MAX_TIME_LENGTH),
                new OptionData(OptionType.STRING, TYPE_ID, "Type of event. Assignment, Quiz, Midterm, etc.", true)
                    .addChoices(
                        Arrays.stream(EventJpa.EventType.values())
                            .map(type -> new Choice(type.displayName(), type.name()))
                            .toList()
                    )
            ),
            Commands.slash(Command.REMINDER_OFFSETS_CONFIG.getDisplayName(), Command.DESCRIPTIONS.get(Command.REMINDER_OFFSETS_CONFIG.getDisplayName()))
                .addSubcommands(
                    new SubcommandData(StudentService.ReminderConfigOptions.LIST.toLower(), "List current reminder offsets"),
                    new SubcommandData(StudentService.ReminderConfigOptions.ADD.toLower(), "Add a new reminder offset")
                        .addOptions(
                            new OptionData(OptionType.INTEGER, OFFSET_ID, "New offset in hours that you would like to be reminded before an event.", true)
                                .setMinValue(MIN_OFFSET_VALUE)
                                .setRequired(true)
                        ),
                    new SubcommandData(StudentService.ReminderConfigOptions.REMOVE.toLower(), "Remove a reminder offset")
                        .addOptions(
                            new OptionData(OptionType.INTEGER, OFFSET_ID, "Offset you wish to remove", true)
                                .setMinValue(MIN_OFFSET_VALUE)
                                .setRequired(true)
                                .setAutoComplete(true)
                        )

                ),
            Commands.slash(Command.VIEW_EVENTS.getDisplayName(), Command.DESCRIPTIONS.get(Command.VIEW_EVENTS.getDisplayName())).addOptions(
                new OptionData(OptionType.INTEGER, UPCOMING_ID, "Get a list of the next X upcoming events.", false)
                    .setMinValue(MIN_FILTER_VALUE),
                new OptionData(OptionType.INTEGER, TIME_PERIOD_ID, "Filter events to a certain time period in the next X days.", false)
                    .setMinValue(MIN_FILTER_VALUE),
                new OptionData(OptionType.STRING, COURSE_ID, "Filter events for a certain course.", false)
                    .setAutoComplete(true)
            )
        ).queue();
    }
}
