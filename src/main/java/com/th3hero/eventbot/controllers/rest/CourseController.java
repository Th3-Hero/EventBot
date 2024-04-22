package com.th3hero.eventbot.controllers.rest;

import com.th3hero.eventbot.dto.course.Course;
import com.th3hero.eventbot.dto.course.CourseUpload;
import com.th3hero.eventbot.dto.course.CourseUploadUpdate;
import com.th3hero.eventbot.services.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/course")
@Tag(name = "Course Controller", description = "Handles admin operations regarding Courses")
public class CourseController {
    private final CourseService courseService;

    @GetMapping("/list")
    @Operation(summary = "Returns a list of all courses")
    public Collection<Course> listCourses() {
        return courseService.getAllCourses();
    }

    @PostMapping("/create")
    @Operation(summary = "Create a new course")
    @ResponseStatus(HttpStatus.CREATED)
    public Course createCourse(
        @RequestBody @NotNull CourseUpload courseUpload
    ) {
        return courseService.createCourse(courseUpload);
    }

    @PostMapping("/{courseId}")
    @Operation(summary = "Update the information of an course")
    public Course updateCourse(
        @PathVariable @NotNull Integer courseId,
        @RequestBody @NotNull CourseUploadUpdate courseUploadUpdate
    ) {
        return courseService.updateCourse(courseId, courseUploadUpdate);
    }

    @DeleteMapping("/{courseId}")
    @Operation(summary = "Delete a Course by its id")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourse(
            @PathVariable @NotNull Integer courseId
    ) {
        courseService.deleteCourseById(courseId);
    }
}
