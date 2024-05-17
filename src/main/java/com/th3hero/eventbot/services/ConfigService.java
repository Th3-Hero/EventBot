package com.th3hero.eventbot.services;

import com.th3hero.eventbot.dto.config.Config;
import com.th3hero.eventbot.dto.config.ConfigUpload;
import com.th3hero.eventbot.dto.config.ConfigUploadUpdate;
import com.th3hero.eventbot.entities.ConfigJpa;
import com.th3hero.eventbot.repositories.ConfigRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ConfigService {
    private final ConfigRepository configRepository;

    public ConfigJpa getConfigJpa() {
        List<ConfigJpa> configList = configRepository.findAll();
        if (configList.isEmpty()) {
            throw new EntityNotFoundException("No existing config was found. Contact the bot owner to fix this issue.");
        }
        if (configList.size() > 1) {
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
            .eventChannel(configUpload.eventChannel())
            .botOwnerId(configUpload.botOwnerId());

        if (configUpload.deletedEventCleanupDelay() != null) {
            configJpaBuilder.deletedEventCleanupDelay(configUpload.deletedEventCleanupDelay());
        }
        if (configUpload.draftCleanupDelay() != null) {
            configJpaBuilder.draftCleanupDelay(configUpload.draftCleanupDelay());
        }

        return configRepository.save(configJpaBuilder.build()).toDto();
    }

    public Config updateConfig(ConfigUploadUpdate configUpload) {
        ConfigJpa configJpa = getConfigJpa();

        if (configUpload.eventChannel() != null) {
            configJpa.setEventChannel(configUpload.eventChannel());
        }
        if (configUpload.botOwnerId() != null) {
            configJpa.setBotOwnerId(configUpload.botOwnerId());
        }
        if (configUpload.deletedEventCleanupDelay() != null) {
            configJpa.setDeletedEventCleanupDelay(configUpload.deletedEventCleanupDelay());
        }
        if (configUpload.draftCleanupDelay() != null) {
            configJpa.setDraftCleanupDelay(configUpload.draftCleanupDelay());
        }

        return configRepository.save(configJpa).toDto();
    }

}
