package com.th3hero.eventbot.entities;

import java.io.Serializable;
import java.util.UUID;

import com.th3hero.eventbot.dto.Course;
import jakarta.persistence.*;
import lombok.*;


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
    @NonNull
    @Setter(AccessLevel.NONE)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

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
