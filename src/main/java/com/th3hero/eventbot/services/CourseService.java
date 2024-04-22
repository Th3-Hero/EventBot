package com.th3hero.eventbot.services;

import com.kseth.development.util.CollectionUtils;
import com.th3hero.eventbot.commands.CommandRequest;
import com.th3hero.eventbot.commands.Selection;
import com.th3hero.eventbot.commands.SelectionRequest;
import com.th3hero.eventbot.dto.course.Course;
import com.th3hero.eventbot.dto.course.CourseUpload;
import com.th3hero.eventbot.dto.course.CourseUploadUpdate;
import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.entities.StudentJpa;
import com.th3hero.eventbot.repositories.CourseRepository;
import com.th3hero.eventbot.utils.EmbedBuilderFactory;
import com.th3hero.eventbot.utils.HttpErrorUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;
    private final StudentService studentService;

    public Collection<Course> getAllCourses() {
        return CollectionUtils.transform(
                courseRepository.findAll(),
                CourseJpa::toDto
        );
    }

    public Course createCourse(CourseUpload courseUpload) {
        return courseRepository.save(courseUpload.toJpa()).toDto();
    }

    public Course updateCourse(Integer courseId, CourseUploadUpdate courseUpload) {
        CourseJpa courseJpa = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException(HttpErrorUtil.MISSING_COURSE_WITH_ID));

        if (courseUpload.code() != null) {
            courseJpa.setCode(courseUpload.code());
        }
        if (courseUpload.name() != null) {
            courseJpa.setName(courseUpload.name());
        }
        if (courseUpload.nickname() != null) {
            courseJpa.setNickname(courseUpload.nickname());
        }

        return courseRepository.save(courseJpa).toDto();
    }

    public void deleteCourseById(Integer courseId) {
        CourseJpa courseJpa = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException(HttpErrorUtil.MISSING_COURSE_WITH_ID));

        List<StudentJpa> students = studentService.fetchAllStudents();
        for (StudentJpa student : students) {
            student.getCourses().remove(courseJpa);
        }

        courseRepository.deleteById(courseId);
    }

    private List<SelectOption> selectOptionsFromJpas(List<CourseJpa> courses) {
        return courses.stream()
                .map(course -> SelectOption.of(course.getCode(), course.getCode()).withDescription(course.getName()))
                .toList();
    }

    public void sendCourseSelectionMenu(CommandRequest request) {
        StudentJpa studentJpa = studentService.fetchStudent(request.requester().getIdLong());

        List<SelectOption> options = selectOptionsFromJpas(courseRepository.findAll());
        if (options.isEmpty()) {
            request.event().reply("No courses are currently setup. Ask William.").setEphemeral(true).queue();
        }

        StringSelectMenu menu = StringSelectMenu.create(Selection.SELECT_COURSES.toString())
                .setPlaceholder("Select Courses")
                .setMaxValues(options.size())
                .addOptions(options)
                .setDefaultValues(studentJpa.getCourses().stream().map(CourseJpa::getCode).toList())
                .build();

        request.event().replyEmbeds(EmbedBuilderFactory.coursePicker())
                .addActionRow(menu)
                .setEphemeral(true)
                .queue();
    }

    public void processCourseSelection(SelectionRequest request) {
        StudentJpa studentJpa = studentService.fetchStudent(request.requester().getIdLong());

        List<String> values = request.interaction().getValues();
        List<CourseJpa> selectedCourseJpas = new ArrayList<>();
        for (String courseCode : values) {
            Optional<CourseJpa> course = courseRepository.findCourseJpaByCode(courseCode);
            if (course.isEmpty()) {
                request.interaction().reply("One or more of the courses selected is not within the database. Please use /select_courses for an updated selector.")
                        .setEphemeral(true)
                        .queue();
                return;
            }
            selectedCourseJpas.add(course.get());
        }

        studentJpa.getCourses().clear();
        studentJpa.getCourses().addAll(selectedCourseJpas);

        request.interaction().replyEmbeds(EmbedBuilderFactory.selectedCourses(studentJpa.getCourses()))
                .setEphemeral(true)
                .queue();
    }

}
