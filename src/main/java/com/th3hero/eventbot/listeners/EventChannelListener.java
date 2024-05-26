package com.th3hero.eventbot.listeners;

import com.th3hero.eventbot.listeners.events.UpdatedEventChannelEvent;
import com.th3hero.eventbot.services.EventService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventChannelListener {
    private final EventService eventService;
    private final JDA jda;

    /**
     * Listens for UpdatedEventChannelEvent and sends all events to the event channel
     * @param event UpdatedEventChannelEvent
     */
    @EventListener
    public void updatedEventChannelListener(UpdatedEventChannelEvent event) {
        eventService.sendAllEventsToEventChannel(jda);
    }
}
