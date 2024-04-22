package com.th3hero.eventbot.entities;

import com.th3hero.eventbot.dto.config.Config;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;

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

    @NotNull
    @Column
    private String term;

    public Config toDto() {
        return new Config(
                this.getId(),
                this.getEventChannel(),
                this.getTerm()
        );
    }
}
