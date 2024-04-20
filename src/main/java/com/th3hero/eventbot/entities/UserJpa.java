package com.th3hero.eventbot.entities;

import java.util.List;
import java.io.Serializable;

import lombok.*;
import jakarta.persistence.*;


@Getter
@Setter
@Entity
@Builder
@ToString
@Table(name = "user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class UserJpa implements Serializable {

    @Id
    @NonNull
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private String id;

    @OneToMany
    private List<CourseJpa> courses;
}
