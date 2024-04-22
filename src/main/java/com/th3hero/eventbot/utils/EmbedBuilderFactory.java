package com.th3hero.eventbot.utils;

import com.th3hero.eventbot.commands.Command;
import com.th3hero.eventbot.dto.config.Config;
import com.th3hero.eventbot.entities.CourseJpa;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class EmbedBuilderFactory {
    private static final Color BLUE = new Color(3, 123, 252);

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

    public static MessageEmbed coursePicker() {
        return new EmbedBuilder()
                .setColor(BLUE)
                .setTitle("Course Selection")
                .setDescription("Select Any courses you wish to receive notifications for.")
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
}
