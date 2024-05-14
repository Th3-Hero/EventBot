package com.th3hero.eventbot.repositories;

import com.th3hero.eventbot.entities.ConfigJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfigRepository extends JpaRepository<ConfigJpa, Long> {
}
