package com.khronodragon.bluestone;

import net.dv8tion.jda.client.events.call.CallCreateEvent;
import net.dv8tion.jda.client.events.call.CallDeleteEvent;
import net.dv8tion.jda.client.events.call.update.CallUpdateRingingUsersEvent;
import net.dv8tion.jda.client.events.call.voice.*;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.events.*;
import net.dv8tion.jda.core.events.guild.*;
import net.dv8tion.jda.core.events.guild.member.*;
import net.dv8tion.jda.core.events.guild.voice.*;
import net.dv8tion.jda.core.events.message.*;
import net.dv8tion.jda.core.events.message.guild.*;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveAllEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.core.events.message.priv.*;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveAllEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.EventListener;

public interface EventedCog extends EventListener {
    default boolean needsThreadedEvents() {
        return false;
    }
    //JDA Events
    default void onReady(ReadyEvent event) {}
    default void onReconnect(ReconnectedEvent event) {}
    default void onDisconnect(DisconnectEvent event) {}
    default void onShutdown(ShutdownEvent event) {}
    default void onStatusChange(StatusChangeEvent event) {}
    default void onException(ExceptionEvent event) {}

    //Message Events
    //Guild (TextChannel) Message Events
    default void onGuildMessageReceived(GuildMessageReceivedEvent event) {}
    default void onGuildMessageDelete(GuildMessageDeleteEvent event) {}
    default void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {}
    default void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {}
    default void onGuildMessageReactionRemoveAll(GuildMessageReactionRemoveAllEvent event) {}

    //Private Message Events
    default void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {}
    default void onPrivateMessageUpdate(PrivateMessageUpdateEvent event) {}

    //Combined Message Events (Combines Guild and Private message into 1 event)
    default void onMessageReceived(MessageReceivedEvent event) {}
    default void onMessageDelete(MessageDeleteEvent event) {}
    default void onMessageBulkDelete(MessageBulkDeleteEvent event) {}
    default void onMessageEmbed(MessageEmbedEvent event) {}
    default void onMessageReactionAdd(MessageReactionAddEvent event) {}
    default void onMessageReactionRemove(MessageReactionRemoveEvent event) {}
    default void onMessageReactionRemoveAll(MessageReactionRemoveAllEvent event) {}

    //Guild Events
    default void onGuildJoin(GuildJoinEvent event) {}
    default void onGuildLeave(GuildLeaveEvent event) {}
    default void onUnavailableGuildJoined(UnavailableGuildJoinedEvent event) {}

    //Guild Member Events
    default void onGuildMemberJoin(GuildMemberJoinEvent event) {}
    default void onGuildMemberLeave(GuildMemberLeaveEvent event) {}
    default void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {}
    default void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {}
    default void onGuildMemberNickChange(GuildMemberNickChangeEvent event) {}

    //Guild Voice Events
    default void onGuildVoiceJoin(GuildVoiceJoinEvent event) {}
    default void onGuildVoiceMove(GuildVoiceMoveEvent event) {}
    default void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {}
    default void onGuildVoiceMute(GuildVoiceMuteEvent event) {}
    default void onGuildVoiceDeafen(GuildVoiceDeafenEvent event) {}
    default void onGuildVoiceGuildMute(GuildVoiceGuildMuteEvent event) {}
    default void onGuildVoiceGuildDeafen(GuildVoiceGuildDeafenEvent event) {}
    default void onGuildVoiceSelfMute(GuildVoiceSelfMuteEvent event) {}
    default void onGuildVoiceSelfDeafen(GuildVoiceSelfDeafenEvent event) {}
    default void onGuildVoiceSuppress(GuildVoiceSuppressEvent event) {}

    // ==========================================================================================
    // |                                   Client Only Events                                   |
    // ==========================================================================================

    //Call Events
    default void onCallCreate(CallCreateEvent event) {}
    default void onCallDelete(CallDeleteEvent event) {}
    default void onCallUpdateRingingUsers(CallUpdateRingingUsersEvent event) {}

    //Call Voice Events
    default void onCallVoiceJoin(CallVoiceJoinEvent event) {}
    default void onCallVoiceLeave(CallVoiceLeaveEvent event) {}

    default void onEvent(Event event) {
        //JDA Events
        if (event instanceof ReadyEvent)
            onReady((ReadyEvent) event);
        else if (event instanceof ReconnectedEvent)
            onReconnect((ReconnectedEvent) event);
        else if (event instanceof DisconnectEvent)
            onDisconnect((DisconnectEvent) event);
        else if (event instanceof ShutdownEvent)
            onShutdown((ShutdownEvent) event);
        else if (event instanceof StatusChangeEvent)
            onStatusChange((StatusChangeEvent) event);
        else if (event instanceof ExceptionEvent)
            onException((ExceptionEvent) event);

            //Message Events
            //Guild (TextChannel) Message Events
        else if (event instanceof GuildMessageReceivedEvent)
            onGuildMessageReceived((GuildMessageReceivedEvent) event);
        else if (event instanceof GuildMessageDeleteEvent)
            onGuildMessageDelete((GuildMessageDeleteEvent) event);
        else if (event instanceof GuildMessageReactionAddEvent)
            onGuildMessageReactionAdd((GuildMessageReactionAddEvent) event);
        else if (event instanceof GuildMessageReactionRemoveEvent)
            onGuildMessageReactionRemove((GuildMessageReactionRemoveEvent) event);
        else if (event instanceof GuildMessageReactionRemoveAllEvent)
            onGuildMessageReactionRemoveAll((GuildMessageReactionRemoveAllEvent) event);

            //Private Message Events
        else if (event instanceof PrivateMessageReceivedEvent)
            onPrivateMessageReceived((PrivateMessageReceivedEvent) event);
        else if (event instanceof PrivateMessageUpdateEvent)
            onPrivateMessageUpdate((PrivateMessageUpdateEvent) event);

            //Combined Message Events (Combines Guild and Private message into 1 event)
        else if (event instanceof MessageReceivedEvent)
            onMessageReceived((MessageReceivedEvent) event);
        else if (event instanceof MessageDeleteEvent)
            onMessageDelete((MessageDeleteEvent) event);
        else if (event instanceof MessageBulkDeleteEvent)
            onMessageBulkDelete((MessageBulkDeleteEvent) event);
        else if (event instanceof MessageEmbedEvent)
            onMessageEmbed((MessageEmbedEvent) event);
        else if (event instanceof MessageReactionAddEvent)
            onMessageReactionAdd((MessageReactionAddEvent) event);
        else if (event instanceof MessageReactionRemoveEvent)
            onMessageReactionRemove((MessageReactionRemoveEvent) event);
        else if (event instanceof MessageReactionRemoveAllEvent)
            onMessageReactionRemoveAll((MessageReactionRemoveAllEvent) event);

            //Guild Events
        else if (event instanceof GuildJoinEvent)
            onGuildJoin((GuildJoinEvent) event);
        else if (event instanceof GuildLeaveEvent)
            onGuildLeave((GuildLeaveEvent) event);
        else if (event instanceof UnavailableGuildJoinedEvent)
            onUnavailableGuildJoined((UnavailableGuildJoinedEvent) event);

            //Guild Member Events
        else if (event instanceof GuildMemberJoinEvent)
            onGuildMemberJoin((GuildMemberJoinEvent) event);
        else if (event instanceof GuildMemberLeaveEvent)
            onGuildMemberLeave((GuildMemberLeaveEvent) event);
        else if (event instanceof GuildMemberRoleAddEvent)
            onGuildMemberRoleAdd((GuildMemberRoleAddEvent) event);
        else if (event instanceof GuildMemberRoleRemoveEvent)
            onGuildMemberRoleRemove((GuildMemberRoleRemoveEvent) event);
        else if (event instanceof GuildMemberNickChangeEvent)
            onGuildMemberNickChange((GuildMemberNickChangeEvent) event);

            //Guild Voice Events
        else if (event instanceof GuildVoiceJoinEvent)
            onGuildVoiceJoin((GuildVoiceJoinEvent) event);
        else if (event instanceof GuildVoiceMoveEvent)
            onGuildVoiceMove((GuildVoiceMoveEvent) event);
        else if (event instanceof GuildVoiceLeaveEvent)
            onGuildVoiceLeave((GuildVoiceLeaveEvent) event);
        else if (event instanceof GuildVoiceMuteEvent)
            onGuildVoiceMute((GuildVoiceMuteEvent) event);
        else if (event instanceof GuildVoiceDeafenEvent)
            onGuildVoiceDeafen((GuildVoiceDeafenEvent) event);
        else if (event instanceof GuildVoiceGuildMuteEvent)
            onGuildVoiceGuildMute((GuildVoiceGuildMuteEvent) event);
        else if (event instanceof GuildVoiceGuildDeafenEvent)
            onGuildVoiceGuildDeafen((GuildVoiceGuildDeafenEvent) event);
        else if (event instanceof GuildVoiceSelfMuteEvent)
            onGuildVoiceSelfMute((GuildVoiceSelfMuteEvent) event);
        else if (event instanceof GuildVoiceSelfDeafenEvent)
            onGuildVoiceSelfDeafen((GuildVoiceSelfDeafenEvent) event);
        else if (event instanceof GuildVoiceSuppressEvent)
            onGuildVoiceSuppress((GuildVoiceSuppressEvent) event);

        if (event.getJDA().getAccountType() == AccountType.CLIENT) {
            //Call Events
            if (event instanceof CallCreateEvent)
                onCallCreate((CallCreateEvent) event);
            else if (event instanceof CallDeleteEvent)
                onCallDelete((CallDeleteEvent) event);
            else if (event instanceof CallUpdateRingingUsersEvent)
                onCallUpdateRingingUsers((CallUpdateRingingUsersEvent) event);

                //Call Voice Events
            else if (event instanceof CallVoiceJoinEvent)
                onCallVoiceJoin((CallVoiceJoinEvent) event);
            else if (event instanceof CallVoiceLeaveEvent)
                onCallVoiceLeave((CallVoiceLeaveEvent) event);
        }
    }
}
