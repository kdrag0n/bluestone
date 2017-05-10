package com.khronodragon.glabot

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class Command(cmdName: String, cmdDesc: String,
              cmdUsage: String = "", cmdHidden: Boolean = false,
              cmdNPerms: Array[String] = Array[String](), cmdNoPm: Boolean = false,
              cmdAliases: Array[String] = Array[String](), cmdCall: Function[Context, _]) {
    final val name: String = cmdName
    final val description: String = cmdDesc
    final val usage: String = cmdUsage
    final val hidden: Boolean = cmdHidden
    final val permsRequired: Array[String] = cmdNPerms
    final val noPm: Boolean = cmdNoPm
    final val aliases: Array[String] = cmdAliases
    final val function: Function[Context, _] = cmdCall

    def invoke(bot: Bot, event: MessageReceivedEvent, args: Array[String], prefix: String, invokedName: String): Unit = {
        val context = new Context(bot, event, args, prefix, invokedName)
        this.function(context)
    }
}
