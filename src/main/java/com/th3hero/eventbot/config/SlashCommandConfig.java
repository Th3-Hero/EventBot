package com.th3hero.eventbot.config;

import com.th3hero.eventbot.commands.actions.Command;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.formatting.DateFormatter;
import com.th3hero.eventbot.services.StudentService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
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
            Commands.slash(Command.TEST_NOTIFICATION.getDisplayName(), Command.DESCRIPTIONS.get(Command.TEST_NOTIFICATION.getDisplayName())),
            Commands.slash(Command.SELECT_COURSES.getDisplayName(), Command.DESCRIPTIONS.get(Command.SELECT_COURSES.getDisplayName())),
            Commands.slash(Command.MY_COURSES.getDisplayName(), Command.DESCRIPTIONS.get(Command.MY_COURSES.getDisplayName())),
            Commands.slash(Command.CREATE_EVENT.getDisplayName(), Command.DESCRIPTIONS.get(Command.CREATE_EVENT.getDisplayName())).addOptions(
                new OptionData(OptionType.STRING, DATE, DateFormatter.DATE_FORMAT_EXAMPLE, true)
                    .setRequiredLength(MIN_DATE_LENGTH, MAX_DATE_LENGTH),
                new OptionData(OptionType.STRING, TIME, DateFormatter.TIME_FORMAT_EXAMPLE, true)
                    .setRequiredLength(MIN_TIME_LENGTH, MAX_TIME_LENGTH),
                new OptionData(OptionType.STRING, TYPE, "Type of event. Assignment, Quiz, Midterm, etc.", true)
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
                            new OptionData(OptionType.INTEGER, OFFSET, "New offset in hours that you would like to be reminded before an event.", true)
                                .setMinValue(MIN_OFFSET_VALUE)
                                .setRequired(true)
                        ),
                    new SubcommandData(StudentService.ReminderConfigOptions.REMOVE.toLower(), "Remove a reminder offset")
                        .addOptions(
                            new OptionData(OptionType.INTEGER, OFFSET, "Offset you wish to remove", true)
                                .setMinValue(MIN_OFFSET_VALUE)
                                .setRequired(true)
                                .setAutoComplete(true)
                        )

                ),
            Commands.slash(Command.VIEW_EVENTS.getDisplayName(), Command.DESCRIPTIONS.get(Command.VIEW_EVENTS.getDisplayName())).addOptions(
                new OptionData(OptionType.INTEGER, UPCOMING, "Get a list of the next X upcoming events.", false)
                    .setMinValue(MIN_FILTER_VALUE)
                    .setMaxValue(MessageEmbed.MAX_FIELD_AMOUNT),
                new OptionData(OptionType.INTEGER, TIME_PERIOD, "Filter events to a certain time period in the next X days.", false)
                    .setMinValue(MIN_FILTER_VALUE),
                new OptionData(OptionType.STRING, COURSE, "Filter events for a certain course.", false)
                    .setAutoComplete(true)
            )
        ).queue();
    }
}
