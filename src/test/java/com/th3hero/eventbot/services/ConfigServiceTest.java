package com.th3hero.eventbot.services;

import com.th3hero.eventbot.TestEntities;
import com.th3hero.eventbot.dto.config.ConfigUpdate;
import com.th3hero.eventbot.entities.ConfigJpa;
import com.th3hero.eventbot.repositories.ConfigRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {
    @Mock
    private ConfigRepository configRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private ConfigService configService;

    @Test
    void getConfigJpa() {
        final ConfigJpa configJpa = TestEntities.configJpa();

        when(configRepository.findAll())
            .thenReturn(List.of(configJpa));

        final var result = configService.getConfigJpa();

        assertThat(result).isEqualTo(configJpa);
    }

    @Test
    void getConfigJpa_noConfig() {
        when(configRepository.findAll())
            .thenReturn(List.of());

        assertThatExceptionOfType(EntityNotFoundException.class)
            .isThrownBy(() -> configService.getConfigJpa());
    }

    @Test
    void getConfigJpa_multipleConfigs() {
        final ConfigJpa configJpa = TestEntities.configJpa();

        when(configRepository.findAll())
            .thenReturn(List.of(configJpa, configJpa));

        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> configService.getConfigJpa());
    }

    @Test
    void createConfig() {
        when(configRepository.findAll())
            .thenReturn(List.of());
        when(configRepository.save(any(ConfigJpa.class))
        ).thenReturn(TestEntities.configJpa());

        final var result = configService.createConfig(TestEntities.configUpload());

        verify(configRepository).save(any(ConfigJpa.class));
        assertThat(result).isNotNull();
    }

    @Test
    void createConfig_existingConfig() {
        final var configJpa = TestEntities.configJpa();
        final var configUpload = TestEntities.configUpload();

        when(configRepository.findAll())
            .thenReturn(List.of(configJpa));

        assertThatExceptionOfType(EntityExistsException.class)
            .isThrownBy(() -> configService.createConfig(configUpload));

        verify(configRepository, never()).save(any());
    }

    @Test
    void createConfig_useDefaults() {
        final var configUpload = TestEntities.configUpload_nullDefaults();

        when(configRepository.findAll())
            .thenReturn(List.of());
        when(configRepository.save(any(ConfigJpa.class)))
            .thenReturn(TestEntities.configJpa());

        final var result = configService.createConfig(configUpload);

        verify(configRepository).save(argThat(configJpa -> configJpa.getDeletedEventCleanupDelay() != null));
        verify(configRepository).save(argThat(configJpa -> configJpa.getDraftCleanupDelay() != null));
    }

    @Test
    void updateConfig() {
        final var configJpa = TestEntities.configJpa();
        final var configUpdate = new ConfigUpdate(69420L, 52L, 69, 420);

        when(configRepository.findAll())
            .thenReturn(List.of(configJpa));
        when(configRepository.save(any(ConfigJpa.class)))
            .thenReturn(configJpa);

        final var result = configService.updateConfig(configUpdate);

        verify(configRepository, times(2)).save(argThat(config ->
            config.getServerId().equals(configUpdate.serverId()) &&
            config.getEventChannel().equals(configUpdate.eventChannel()) &&
            config.getDeletedEventCleanupDelay().equals(configUpdate.deletedEventCleanupDelay()) &&
            config.getDraftCleanupDelay().equals(configUpdate.draftCleanupDelay())
        ));
        assertThat(result).isNotNull();
    }

    @Test
    void updateConfig_updatingServerId() {
        final var configUpdate = new ConfigUpdate(69420L, null, null, null);

        when(configRepository.findAll())
            .thenReturn(List.of(TestEntities.configJpa()));
        when(configRepository.save(any(ConfigJpa.class)))
            .thenReturn(TestEntities.configJpa());

        final var result = configService.updateConfig(configUpdate);

        verify(configRepository).save(argThat(config ->
            config.getServerId().equals(configUpdate.serverId())
        ));
        assertThat(result).isNotNull();
    }

    @Test
    void updateConfig_updatingEventChannel() {
        final var configUpdate = new ConfigUpdate(null, 69420L, null, null);

        when(configRepository.findAll())
            .thenReturn(List.of(TestEntities.configJpa()));
        when(configRepository.save(any(ConfigJpa.class)))
            .thenReturn(TestEntities.configJpa());

        final var result = configService.updateConfig(configUpdate);

        verify(configRepository, times(2)).save(argThat(config ->
            config.getEventChannel().equals(configUpdate.eventChannel())
        ));
        assertThat(result).isNotNull();
    }

    @Test
    void updateConfig_updatingDeletedEventCleanupDelay() {
        final var configUpdate = new ConfigUpdate(null, null, 128, null);

        when(configRepository.findAll())
            .thenReturn(List.of(TestEntities.configJpa()));
        when(configRepository.save(any(ConfigJpa.class)))
            .thenReturn(TestEntities.configJpa());

        final var result = configService.updateConfig(configUpdate);

        verify(configRepository).save(argThat(config ->
            config.getDeletedEventCleanupDelay().equals(configUpdate.deletedEventCleanupDelay())
        ));
        assertThat(result).isNotNull();
    }

    @Test
    void updateConfig_updatingDraftCleanupDelay() {
        final var configUpdate = new ConfigUpdate(null, null, null, 128);

        when(configRepository.findAll())
            .thenReturn(List.of(TestEntities.configJpa()));
        when(configRepository.save(any(ConfigJpa.class)))
            .thenReturn(TestEntities.configJpa());

        final var result = configService.updateConfig(configUpdate);

        verify(configRepository).save(argThat(config ->
            config.getDraftCleanupDelay().equals(configUpdate.draftCleanupDelay())
        ));
        assertThat(result).isNotNull();
    }

}