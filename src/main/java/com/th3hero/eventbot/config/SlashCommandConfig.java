package com.th3hero.eventbot.config;

import com.th3hero.eventbot.commands.Command;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SlashCommandConfig {
    private final JDA jda;

    @EventListener(ApplicationReadyEvent.class)
    public void registerSlashCommands() {
        jda.updateCommands().addCommands(
                Commands.slash(Command.HELP.getDisplayName(), Command.DESCRIPTIONS.get(Command.HELP.getDisplayName())),
                Commands.slash(Command.SELECT_COURSES.getDisplayName(), Command.DESCRIPTIONS.get(Command.SELECT_COURSES.getDisplayName())),
                Commands.slash(Command.MY_COURSES.getDisplayName(), Command.DESCRIPTIONS.get(Command.MY_COURSES.getDisplayName()))
        ).queue();
    }
}
