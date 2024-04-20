package com.th3hero.eventbot.entities;

import com.th3hero.eventbot.dto.Course;
import com.th3hero.eventbot.dto.Event;
import lombok.*;
import jakarta.persistence.*;

import java.util.UUID;
import java.util.Date;
import java.io.Serializable;

@Getter
@Setter
@Entity
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class EventJpa implements Serializable {

    @Id
    @NonNull
    @Setter(AccessLevel.NONE)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @NonNull
    @Column
    private String title;

    @Column
    private String description;

    @NonNull
    @Column
    private Date datetime;

    @ManyToOne
    private CourseJpa course;

    @NonNull
    @Enumerated(EnumType.STRING)
    @Column
    private EventType type;

    public enum EventType {
        ASSIGNMENT,
        LAB,
        EXAM,
        OTHER
    }

    public Event toDto() {
        return new Event(
                this.getId(),
                this.getTitle(),
                this.getDescription(),
                this.getDatetime(),
                course.toDto(),
                this.getType()
        );
    }
}
