package com.th3hero.eventbot.controllers;

import com.th3hero.eventbot.dto.Course;
import com.th3hero.eventbot.dto.CourseUpload;
import com.th3hero.eventbot.services.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
        @RequestBody CourseUpload courseUpload
    ) {
        return courseService.createCourse(courseUpload);
    }

//    @PostMapping("/{courseId}")
//    @Operation(summary = "Update the information of an course")
//    public Course updateCourse(
//        @PathVariable Integer courseId,
//        @RequestBody CourseUpload courseUpload
//    ) {
//        return courseService.updateCourse(courseId, courseUpload);
//    }
    
    @DeleteMapping("/{courseId}")
    @Operation(summary = "Delete a Course by its id")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourse(
            @PathVariable Integer courseId
    ) {
        courseService.deleteCourseById(courseId);
    }
}
