package com.th3hero.eventbot.listeners;

import com.th3hero.eventbot.listeners.handlers.DeletionHandler;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeletionListener extends ListenerAdapter {
    private final DeletionHandler deletionHandler;

    @Override
    public void onMessageDelete(@NonNull MessageDeleteEvent event) {
        deletionHandler.handleDeletedMessage(event);
    }

    @Override
    public void onChannelDelete(@NonNull ChannelDeleteEvent event) {
        deletionHandler.handleDeletedChannel(event);
    }
}
