package com.th3hero.eventbot.listeners;

import com.th3hero.eventbot.services.DeletionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeletionListener extends ListenerAdapter {
    private final DeletionHandler deletionHandler;

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        deletionHandler.handleDeletedMessage(event);
    }

    @Override
    public void onChannelDelete(ChannelDeleteEvent event) {
        deletionHandler.handleDeletedChannel(event);
    }
}
