package com.th3hero.eventbot.controllers.discord;

import com.th3hero.eventbot.commands.requests.CommandRequest;
import com.th3hero.eventbot.exceptions.ConfigErrorException;
import com.th3hero.eventbot.exceptions.DataAccessException;
import com.th3hero.eventbot.exceptions.IllegalInteractionException;
import com.th3hero.eventbot.exceptions.UnsupportedResponseException;
import com.th3hero.eventbot.factories.EmbedBuilderFactory;
import com.th3hero.eventbot.services.*;
import com.th3hero.eventbot.utils.DiscordActionUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Controller;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SlashCommandController extends ListenerAdapter {
    private final ConfigService configService;
    private final CourseService courseService;
    private final StudentService studentService;
    private final EventDraftService eventDraftService;
    private final EventService eventService;

    @Override
    public void onSlashCommandInteraction(@NonNull SlashCommandInteractionEvent event) {
        try {
            final CommandRequest request = CommandRequest.fromInteraction(event);
            commandHandler(request);
        } catch (EntityNotFoundException e) {
            DiscordActionUtils.textResponse(event, e.getMessage(), true);
            log.debug(e.getMessage(), e);
        } catch (DataAccessException | IllegalInteractionException | ConfigErrorException e) {
            DiscordActionUtils.textResponse(event, e.getMessage(), true);
            log.warn(e.getMessage());
        } catch (UnsupportedResponseException e) {
            DiscordActionUtils.textResponse(event, "Failed to respond to the interaction.", true);
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error("Slash Command: {}", event.getName());
            List<String> options = event.getOptions().stream()
                .map(option -> "%s: %s".formatted(option.getName(), option.getAsString()))
                .toList();
            log.error("Options: {}", options);
            DiscordActionUtils.textResponse(event, DiscordActionUtils.DEFAULT_ERROR_RESPONSE, true);
            throw e;
        }
    }

    public void commandHandler(@NotNull final CommandRequest request) {
        request.addEventChannel(configService.getConfigJpa().getEventChannel());
        switch (request.getCommand()) {
            case HELP -> DiscordActionUtils.embedResponse(request.getEvent(), EmbedBuilderFactory.help(), true);
            case SELECT_COURSES -> courseService.sendCourseSelectionMenu(request);
            case MY_COURSES -> studentService.listStudentCourses(request);
            case CREATE_EVENT -> eventDraftService.createEventDraft(request);
            case REMINDER_OFFSETS_CONFIG -> studentService.reminderOffsetSubcommandHandler(request);
            case VIEW_EVENTS -> eventService.filterViewEvents(request);
            default -> log.error("Received an unsupported command: {}", request.getEvent().getName());
        }
    }
}
