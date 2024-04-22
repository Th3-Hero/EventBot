package com.th3hero.eventbot.repositories;

import com.th3hero.eventbot.entities.ConfigJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigRepository extends JpaRepository<ConfigJpa, Long> {
}
