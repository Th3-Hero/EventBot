package com.th3hero.eventbot.entities;

import java.io.Serializable;

import lombok.*;
import jakarta.persistence.*;
import com.th3hero.eventbot.dto.course.Course;


@Getter
@Setter
@Entity
@Builder
@ToString
@Table(name = "course")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseJpa implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_course_id_generator")
    @SequenceGenerator(name = "seq_course_id_generator", sequenceName = "seq_course_id", allocationSize = 1)
    @Setter(AccessLevel.NONE)
    @Column(name = "id")
    private Integer id;

    @NonNull
    @Column
    private String code;

    @Column
    private String name;

    @Column
    private String nickname;

    public Course toDto() {
        return new Course(
                this.getId(),
                this.getCode(),
                this.getName(),
                this.getNickname()
        );
    }
}
