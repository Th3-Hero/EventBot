package com.th3hero.eventbot.entities;

import java.util.List;
import java.io.Serializable;

import lombok.*;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@Entity
@Builder
@ToString
@Table(name = "student")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentJpa implements Serializable {

    @Id
    @NonNull
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Long id;

    @OneToMany
    private List<CourseJpa> courses;

    public static StudentJpa create(
            Long studentId
    ) {
        return StudentJpa.builder()
                .id(studentId)
                .courses(List.of())
                .build();
    }
}
