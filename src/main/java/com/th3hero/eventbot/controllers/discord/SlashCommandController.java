package com.th3hero.eventbot.controllers.discord;

import com.th3hero.eventbot.commands.requests.CommandRequest;
import com.th3hero.eventbot.exceptions.ArgumentMappingException;
import com.th3hero.eventbot.exceptions.UnsupportedInteractionException;
import com.th3hero.eventbot.factories.EmbedBuilderFactory;
import com.th3hero.eventbot.services.CourseService;
import com.th3hero.eventbot.services.EventDraftService;
import com.th3hero.eventbot.services.EventService;
import com.th3hero.eventbot.services.StudentService;
import com.th3hero.eventbot.utils.DiscordActionUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SlashCommandController extends ListenerAdapter {
    private final CourseService courseService;
    private final StudentService studentService;
    private final EventDraftService eventDraftService;
    private final EventService eventService;

    @Override
    public void onSlashCommandInteraction(@NonNull SlashCommandInteractionEvent event) {
        try {
            final CommandRequest request = CommandRequest.fromInteraction(event);
            commandHandler(request);
        } catch (UnsupportedInteractionException | ArgumentMappingException e) {
            DiscordActionUtils.textResponse(event, e.getMessage(), true);
            log.warn(e.getMessage());
        } catch (Exception e) {
            log.error("onSlashCommandInteraction", e);
            DiscordActionUtils.textResponse(event, "An unexpected error occurred while processing your request.", true);
            throw e;
        }
    }

    public void commandHandler(@NotNull final CommandRequest request) {
        try {
            switch (request.getCommand()) {
                case HELP -> DiscordActionUtils.embedResponse(request.getEvent(), EmbedBuilderFactory.help(), true);
                case SELECT_COURSES -> courseService.sendCourseSelectionMenu(request);
                case MY_COURSES -> studentService.myCourses(request);
                case CREATE_EVENT -> eventDraftService.createEventDraft(request);
                case REMINDER_OFFSETS_CONFIG -> studentService.reminderOffsetsHandler(request);
                case VIEW_EVENTS -> eventService.listEvents(request);
                default -> log.warn("Received an unsupported command: {}", request.getEvent().getName());
            }
        } catch (Exception e) {
            log.error("Slash Command Handler", e);
            DiscordActionUtils.textResponse(request.getEvent(), e.getMessage(), true);
            throw e;
        }

    }
}
