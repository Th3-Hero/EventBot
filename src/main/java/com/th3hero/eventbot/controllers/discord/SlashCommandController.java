package com.th3hero.eventbot.controllers.discord;

import com.th3hero.eventbot.commands.CommandRequest;
import com.th3hero.eventbot.services.CourseService;
import com.th3hero.eventbot.services.EventDraftService;
import com.th3hero.eventbot.services.StudentService;
import com.th3hero.eventbot.utils.EmbedBuilderFactory;
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

    @Override
    public void onSlashCommandInteraction(@NonNull SlashCommandInteractionEvent event) {

        try {
            final CommandRequest request = CommandRequest.create(event);
            commandHandler(request);
        } catch (Exception e) {
            log.error("onSlashCommandInteraction", e);
            event.reply(e.getMessage()).setEphemeral(true).queue();
            throw e;
        }
    }

    public void commandHandler(@NotNull final CommandRequest request) {
        try {
            switch (request.command()) {
                case HELP -> request.event().replyEmbeds(EmbedBuilderFactory.help()).setEphemeral(true).queue();
                case SELECT_COURSES -> courseService.sendCourseSelectionMenu(request);
                case MY_COURSES -> studentService.myCourses(request);
                case CREATE_EVENT -> eventDraftService.createEventDraft(request);
                case REMINDER_OFFSETS_CONFIG -> studentService.reminderOffsetsHandler(request);
            }
        } catch (Exception e) {
            request.event().reply(e.getMessage()).setEphemeral(true).queue();
            log.error("Slash Command Handler", e);
            throw e;
        }

    }
}