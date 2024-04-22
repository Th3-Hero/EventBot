package com.th3hero.eventbot.services;

import com.th3hero.eventbot.commands.CommandRequest;
import com.th3hero.eventbot.entities.StudentJpa;
import com.th3hero.eventbot.repositories.StudentRepository;
import com.th3hero.eventbot.utils.EmbedBuilderFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class StudentService {
    private final StudentRepository studentRepository;

    public StudentJpa fetchStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseGet(() -> studentRepository.save(StudentJpa.create(studentId)));
    }

    public List<StudentJpa> fetchAllStudents() {
        return studentRepository.findAll();
    }

    public void myCourses(CommandRequest request) {
        StudentJpa studentJpa = fetchStudent((request.requester().getIdLong()));

        if (studentJpa.getCourses().isEmpty()) {
            request.event().reply("You currently have no selected courses. Please use the /select_courses command.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        request.event().replyEmbeds(EmbedBuilderFactory.selectedCourses(studentJpa.getCourses()))
                .setEphemeral(true)
                .queue();
    }
}
