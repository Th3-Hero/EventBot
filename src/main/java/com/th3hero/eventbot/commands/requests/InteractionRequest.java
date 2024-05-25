package com.th3hero.eventbot.commands.requests;

import com.th3hero.eventbot.exceptions.ConfigErrorException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.util.Optional;
import java.util.function.Consumer;

@Getter
@RequiredArgsConstructor
public abstract class InteractionRequest {
    @NonNull
    protected final Member requester;
    @NonNull
    protected final Guild server;

    @Getter(value = AccessLevel.NONE)
    protected Long eventChannelId = null;

    protected static final String SENT_TO_EVENT_CHANNEL = "Result has been sent to the event channel %s";

    /**
     * Sends a response back to the user that made the request.
     *
     * @param response The response to send
     * @param mode The mode to use when sending the response
     */
    public void sendResponse(@NonNull final Object response, final MessageMode mode) {
        sendResponse(response, mode, null);
    }

    /**
     * Sends a response back to the user that made the request.
     *
     * @param response The response to send
     * @param mode The mode to use when sending the response
     * @param success A callback to execute on response success
     */
    public abstract void sendResponse(
        @NonNull final Object response,
        final MessageMode mode,
        final Consumer<Message> success
    );

    /**
     * Sends a deferred reply to discord
     *
     * @param mode The mode used when later sending the response
     */
    public abstract void deferReply(final MessageMode mode);


    /**
     * Add the event channel id to the request for later use when calling with <code>MessageMode.EVENT_CHANNEL</code>
     *
     * @param channelId event channel id to add
     */
    public void addEventChannel(final Long channelId) {
        this.eventChannelId = channelId;
    }

    public final MessageChannel getEventChannel() {
        if (eventChannelId == null) {
            throw new IllegalStateException("No event channel was found on the request. Make sure to addEventChannel");
        }
        return Optional.ofNullable(server.getTextChannelById(eventChannelId))
            .orElseThrow(() -> new ConfigErrorException("Unable to find event channel %d".formatted(eventChannelId)));
    }


    public enum MessageMode {
        USER,
        EVENT_CHANNEL;
    }
}
