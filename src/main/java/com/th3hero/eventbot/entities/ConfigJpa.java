package com.th3hero.eventbot.entities;

import com.th3hero.eventbot.dto.config.Config;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Entity
@Builder
@Table(name = "config")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ConfigJpa implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_config_id_generator")
    @SequenceGenerator(name = "seq_config_id_generator", sequenceName = "seq_config_id", allocationSize = 1)
    @Setter(AccessLevel.NONE)
    @Column(name = "id")
    private Integer id;

    @NotNull
    @Column
    private Long eventChannel;

    /**
     * The number of hours to wait before cleaning up an event that has been deleted by the user.
     */
    @Builder.Default
    @Column
    private Integer deletedEventCleanupDelay = 48;

    /**
     * The number of hours to wait before cleaning up an unconfirmed draft event.
     */
    @Builder.Default
    @Column
    private Integer draftCleanupDelay = 24;

    public Config toDto() {
        return new Config(
            this.getId(),
            this.getEventChannel(),
            this.getDeletedEventCleanupDelay(),
            this.getDraftCleanupDelay()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        ConfigJpa configJpa = (ConfigJpa) o;
        return id.equals(configJpa.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
