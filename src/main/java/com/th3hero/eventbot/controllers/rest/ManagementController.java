package com.th3hero.eventbot.controllers.rest;

import com.th3hero.eventbot.dto.management.Announcement;
import com.th3hero.eventbot.entities.ConfigJpa;
import com.th3hero.eventbot.exceptions.MissingEventChannelException;
import com.th3hero.eventbot.factories.EmbedBuilderFactory;
import com.th3hero.eventbot.services.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/management")
@Tag(name = "Management Controller", description = "Handles admin operations")
public class ManagementController {
    private final ConfigService configService;
    private final JDA jda;

    @PostMapping
    @Operation(summary = "Create a new announcement")
    @ResponseStatus(HttpStatus.CREATED)
    public void createAnnouncement(
        @RequestBody @NotNull @Valid Announcement announcement
    ) {
        ConfigJpa config = configService.getConfigJpa();

        TextChannel channel = Optional.ofNullable(jda.getTextChannelById(config.getEventChannel()))
            .orElseThrow(() -> new MissingEventChannelException("Failed to find event channel when trying to send announcement"));

        channel.sendMessageEmbeds(EmbedBuilderFactory.announcementEmbed(announcement)).queue();
        log.info("Announcement posted to event channel");
    }
}
