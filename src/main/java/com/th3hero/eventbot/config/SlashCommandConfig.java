package com.th3hero.eventbot.config;

import com.th3hero.eventbot.commands.Command;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SlashCommandConfig {
    private final JDA jda;

    public void registerSlashCommands() {
        jda.updateCommands().addCommands(
                Commands.slash(Command.HELP.toString(), Command.DESCRIPTIONS.get(Command.HELP.toString()))
                        .setGuildOnly(true)
        ).queue();
    }
}
