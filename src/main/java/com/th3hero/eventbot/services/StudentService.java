package com.th3hero.eventbot.services;

import com.kseth.development.util.EnumUtils;
import com.th3hero.eventbot.commands.ButtonRequest;
import com.th3hero.eventbot.commands.CommandRequest;
import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.entities.StudentJpa;
import com.th3hero.eventbot.exceptions.UnsupportedInteractionException;
import com.th3hero.eventbot.repositories.StudentRepository;
import com.th3hero.eventbot.utils.EmbedBuilderFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.th3hero.eventbot.config.DiscordFieldsConfig.OFFSET_ID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class StudentService {
    private final StudentRepository studentRepository;
    private final SchedulingService schedulingService;

    public StudentJpa fetchStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseGet(() -> studentRepository.save(StudentJpa.create(studentId)));
    }

    public List<StudentJpa> fetchAllStudents() {
        return studentRepository.findAll();
    }

    public List<CourseJpa> fetchStudentCourses(Long studentId) {
        return fetchStudent(studentId).getCourses();
    }

    public List<StudentJpa> fetchStudentsWithCourse(CourseJpa courseJpa) {
        return studentRepository.findAllByCoursesContains(courseJpa);
    }

    public void myCourses(CommandRequest request) {
        List<CourseJpa> studentCourses = fetchStudentCourses(request.requester().getIdLong());

        if (studentCourses.isEmpty()) {
            request.event().reply("You currently have no selected courses. Please use the /select_courses command.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        request.event().replyEmbeds(EmbedBuilderFactory.selectedCourses(studentCourses))
                .setEphemeral(true)
                .queue();
    }

    public void offsetAutoComplete(CommandAutoCompleteInteractionEvent event) {
        StudentJpa studentJpa = fetchStudent(event.getUser().getIdLong());
        List<Command.Choice> choices = studentJpa.getOffsetTimes().stream()
                .filter(offset -> offset.toString().startsWith(event.getFocusedOption().getValue()))
                .map(offset -> new Command.Choice(offset.toString(), offset))
                .toList();
        event.replyChoices(choices).queue();
    }

    public void reminderOffsetsHandler(CommandRequest request) {
        ReminderConfigOptions option = EnumUtils.valueOf(
                ReminderConfigOptions.class,
                request.arguments().get("sub_command"),
                new UnsupportedInteractionException("Unknown sub command %s".formatted(request.arguments().get("sub_command")))
        );

        StudentJpa studentJpa = fetchStudent(request.requester().getIdLong());

        switch (option) {
            case LIST -> listReminderOffsets(request, studentJpa);
            case ADD -> addReminderOffsets(request, studentJpa);
            case REMOVE -> removeReminderOffsets(request, studentJpa);
        }
    }

    private void listReminderOffsets(CommandRequest request, StudentJpa studentJpa) {
        studentJpa.getOffsetTimes().sort(null);
        request.event().replyEmbeds(EmbedBuilderFactory.reminderOffsets(studentJpa.getOffsetTimes()))
                .setEphemeral(true)
                .queue();
    }
    private void addReminderOffsets(CommandRequest request, StudentJpa studentJpa) {
        Integer newOffset = Integer.parseInt(request.arguments().get(OFFSET_ID));
        if (studentJpa.getOffsetTimes().contains(newOffset)) {
            request.event().reply("You already have an offset for %d".formatted(newOffset))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        studentJpa.getOffsetTimes().add(newOffset);
        studentJpa.getOffsetTimes().sort(null);
    }
    private void removeReminderOffsets(CommandRequest request, StudentJpa studentJpa) {
        Integer targetOffset = Integer.parseInt(request.arguments().get(OFFSET_ID));
        if (!studentJpa.getOffsetTimes().contains(targetOffset)) {
            request.event().reply("You have no offset for %d".formatted(targetOffset))
                    .setEphemeral(true)
                    .queue();
        }
        studentJpa.getOffsetTimes().remove(targetOffset);
        studentJpa.getOffsetTimes().sort(null);
    }

    public void scheduleStudentForEvent(EventJpa eventJpa, StudentJpa studentJpa) {
        for (Integer offset : studentJpa.getOffsetTimes()) {
            if (eventJpa.getDatetime().minusHours(offset).isBefore(LocalDateTime.now())) {
                continue;
            }
            schedulingService.addEventReminderTrigger(
                    eventJpa.getId(),
                    studentJpa.getId(),
                    offset,
                    eventJpa.getDatetime().minusHours(offset)
            );
        }
    }
    public void unscheduleStudentForEvent(ButtonRequest request, Long eventId) {
        StudentJpa studentJpa = fetchStudent(request.requester().getIdLong());
        boolean removed = schedulingService.removeEventReminderTriggersForStudent(eventId, studentJpa.getId());
        String message = removed ?
                "All reminders have been removed for this event." :
                "You have no reminders on this event.";
        request.buttonInteractionEvent().reply(message).setEphemeral(true).queue();
    }



    public enum ReminderConfigOptions {
        LIST,
        ADD,
        REMOVE;

        @Override
        public String toString() {
            return StringUtils.capitalize(toLower());
        }

        public String toLower() {
            return StringUtils.lowerCase(super.toString());
        }
    }
}
