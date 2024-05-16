package com.th3hero.eventbot.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;


@Getter
@Setter
@Entity
@Builder
@ToString
@Table(name = "event_draft")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class EventDraftJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_event_draft_id_generator")
    @SequenceGenerator(name = "seq_event_draft_id_generator", sequenceName = "seq_event_draft_id", allocationSize = 1)
    @Setter(AccessLevel.NONE)
    @Column(name = "id")
    private Long id;

    @NonNull
    @Column
    private Long authorId;

    @Column
    private String title;

    @Column
    private String note;

    @NonNull
    @Column
    private LocalDateTime datetime;

    @OrderBy("code ASC")
    @ManyToMany
    private List<CourseJpa> courses;

    @NonNull
    @Enumerated(EnumType.STRING)
    @Column
    private EventJpa.EventType type;

    @NonNull
    @Builder.Default
    @Column
    private LocalDateTime draftCreationTime = LocalDateTime.now();

    public static EventDraftJpa create(Long authorId, LocalDateTime eventDate, EventJpa.EventType eventType) {
        return EventDraftJpa.builder()
            .authorId(authorId)
            .datetime(eventDate)
            .type(eventType)
            .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        EventDraftJpa that = (EventDraftJpa) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
