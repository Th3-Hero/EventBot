package com.th3hero.eventbot.services;

import com.th3hero.eventbot.dto.config.Config;
import com.th3hero.eventbot.dto.config.ConfigUpdate;
import com.th3hero.eventbot.dto.config.ConfigUpload;
import com.th3hero.eventbot.entities.ConfigJpa;
import com.th3hero.eventbot.listeners.events.UpdatedEventChannelEvent;
import com.th3hero.eventbot.repositories.ConfigRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ConfigService {
    private final ConfigRepository configRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public ConfigJpa getConfigJpa() {
        List<ConfigJpa> configList = configRepository.findAll();
        if (configList.isEmpty()) {
            log.error("No existing config was found");
            throw new EntityNotFoundException("No existing config was found. Contact the bot owner to fix this issue.");
        }
        // As it stands we should only have one config for the bot
        if (configList.size() > 1) {
            log.error("More then one config was found");
            throw new IllegalStateException("The application has managed to reach an invalid state with multiple configurations. No clue how we got here ¯\\_(ツ)_/¯");
        }

        return configList.getFirst();
    }

    public Config getConfig() {
        return getConfigJpa().toDto();
    }

    public Config createConfig(ConfigUpload configUpload) {
        if (!configRepository.findAll().isEmpty()) {
            throw new EntityExistsException("There is already an existing configuration. Please update it instead of creating a new configuration");
        }

        ConfigJpa.ConfigJpaBuilder configJpaBuilder = ConfigJpa.builder()
            .eventChannel(configUpload.eventChannel());

        if (configUpload.deletedEventCleanupDelay() != null) {
            configJpaBuilder.deletedEventCleanupDelay(configUpload.deletedEventCleanupDelay());
        }
        if (configUpload.draftCleanupDelay() != null) {
            configJpaBuilder.draftCleanupDelay(configUpload.draftCleanupDelay());
        }

        log.debug("Created new config");
        return configRepository.save(configJpaBuilder.build()).toDto();
    }

    public Config updateConfig(ConfigUpdate configUpload) {
        if (configUpload.eventChannel() != null) {
            updateEventChannel(configUpload.eventChannel());
        }

        ConfigJpa configJpa = getConfigJpa();
        if (configUpload.deletedEventCleanupDelay() != null) {
            configJpa.setDeletedEventCleanupDelay(configUpload.deletedEventCleanupDelay());
        }
        if (configUpload.draftCleanupDelay() != null) {
            configJpa.setDraftCleanupDelay(configUpload.draftCleanupDelay());
        }

        log.debug("Updated config");
        return configRepository.save(configJpa).toDto();
    }

    public void updateEventChannel(Long eventChannelId) {
        ConfigJpa configJpa = getConfigJpa();
        configJpa.setEventChannel(eventChannelId);
        configRepository.save(configJpa);
        applicationEventPublisher.publishEvent(new UpdatedEventChannelEvent());
    }

}
