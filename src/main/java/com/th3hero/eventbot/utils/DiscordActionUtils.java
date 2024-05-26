package com.th3hero.eventbot.utils;

import com.th3hero.eventbot.exceptions.IllegalInteractionException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.function.Consumer;

@NoArgsConstructor(access = AccessLevel.NONE)
public final class DiscordActionUtils {

    public static final String DEFAULT_ERROR_RESPONSE = "An unexpected error occurred while processing your request.";

    /**
     * Safely handle sending a text response to an event. The response will be a reply if the event is unacknowledged, otherwise it will use the hook.
     *
     * @param event The event to respond to
     * @param text The text to send
     * @param isUserReply If the response should be ephemeral
     */
    public static <T extends IReplyCallback> void textResponse(
        final T event,
        final String text,
        final boolean isUserReply
    ) {
        if (event.isAcknowledged()) {
            event.getHook().sendMessage(text).setEphemeral(isUserReply).queue();
        } else {
            event.reply(text).setEphemeral(isUserReply).queue();
        }
    }

    /**
     * Safely handle sending an embed response to an event. The response will be a reply if the event is unacknowledged, otherwise it will use the hook.
     *
     * @param event The event to respond to
     * @param embed The embed to send
     * @param isUserReply If the response should be ephemeral
     */
    public static <T extends IReplyCallback> void embedResponse(
        final T event,
        final MessageEmbed embed,
        final boolean isUserReply
    ) {
        if (event.isAcknowledged()) {
            event.getHook().sendMessageEmbeds(embed).setEphemeral(isUserReply).queue();
        } else {
            event.replyEmbeds(embed).setEphemeral(isUserReply).queue();
        }
    }

    /**
     * Safely handle sending a modal response to an event.
     *
     * @param event The event to respond to
     * @param modal The modal to send
     * @throws IllegalInteractionException If the event is already acknowledged
     */
    public static <T extends IModalCallback> void modalResponse(final T event, final Modal modal) {
        if (event.isAcknowledged()) {
            throw new IllegalInteractionException("Cannot send a modal to an acknowledged event.");
        }
        event.replyModal(modal).queue();
    }

    /**
     * Safely handle sending a message create data response to an event. The response will be a reply if the event is unacknowledged, otherwise it will use the hook.
     *
     * @param event The event to respond to
     * @param data The data to send
     * @param isUserReply If the response should be ephemeral
     */
    public static <T extends IReplyCallback> void messageCreateDataResponse(
        final T event,
        final MessageCreateData data,
        final boolean isUserReply
    ) {
        if (event.isAcknowledged()) {
            event.getHook().sendMessage(data).setEphemeral(isUserReply).queue();
        } else {
            event.reply(data).setEphemeral(isUserReply).queue();
        }
    }

    /**
     * Safely handle sending a deferred reply. If the interaction is already acknowledged, then this is a NOOP function.
     *
     * @param event The event to defer
     * @param isUserReply If the chain should be ephemeral
     */
    public static <T extends IReplyCallback> void deferResponse(final T event, final boolean isUserReply) {
        if (!event.isAcknowledged()) {
            event.deferReply(isUserReply).queue();
        }
    }

    /**
     * Retrieves a message from a given channel by its ID.
     *
     * @param channel The channel from which the message will be retrieved.
     * @param messageId The ID of the message to be retrieved.
     * @param success A Consumer to be executed upon successful retrieval of the message.
     * @param error A Consumer to be executed upon encountering an error
     */
    public static void retrieveMessage(
        final MessageChannel channel,
        final Long messageId,
        final Consumer<Message> success,
        final Consumer<? super ErrorResponseException> error
    ) {
        channel.retrieveMessageById(messageId).queue(
            success,
            new ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE, error)
        );
    }

    /**
     * Deletes a message in a given channel by its ID.
     *
     * @param channel The channel where the message to be deleted is located.
     * @param messageId The ID of the message to be deleted.
     * @param success A Consumer to be executed upon successful deletion of the message.
     * @param error A Consumer to be executed upon encountering an error
     */
    public static void deleteMessage(
        final MessageChannel channel,
        final Long messageId,
        final Consumer<Void> success,
        final Consumer<? super ErrorResponseException> error
    ) {
        retrieveMessage(
            channel,
            messageId,
            message -> message.delete().queue(success),
            error
        );
    }

}
