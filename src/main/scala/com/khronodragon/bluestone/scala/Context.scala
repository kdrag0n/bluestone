package com.khronodragon.bluestone

import java.util.Date

import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities._
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.requests.RestAction

class Context(botObj: Bot, mEvent: MessageReceivedEvent, inArgs: Array[String], inPrefix: String, invokedName: String) {
    final val bot: Bot = botObj
    final val event: MessageReceivedEvent = mEvent
    final val message: Message = event.getMessage
    final val author: User = event.getAuthor
    final val responseNumber: Long = event.getResponseNumber
    final val guild: Guild = event.getGuild
    final val server: Guild = guild
    final val channel: MessageChannel = event.getChannel
    final val member: Member = event.getMember
    final val group = event.getGroup
    final val messageId: String = event.getMessageId
    final val messageIdLong: Long = event.getMessageIdLong
    final val textChannel: TextChannel = event.getTextChannel
    final val privateChannel: PrivateChannel = event.getPrivateChannel
    final val jda: JDA = event.getJDA
    final val content: String = message.getRawContent
    final val channelId: String = channel.getId
    final val channelIdLong: Long = channel.getIdLong
    final val prefix: String = inPrefix
    final val args: Array[String] = inArgs
    final val invokedWith: String = invokedName
    final val invokeTime: Date = new Date

    def reply(msg: String): RestAction[Message] = {
        channel.sendMessage(msg)
    }

    def reply(msg: MessageEmbed): RestAction[Message] = {
        channel.sendMessage(msg)
    }

    def reply(msg: Message): RestAction[Message] = {
        channel.sendMessage(msg)
    }
}
