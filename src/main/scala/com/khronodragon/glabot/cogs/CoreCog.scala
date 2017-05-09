package com.khronodragon.glabot.cogs

import com.khronodragon.glabot.Bot
import com.khronodragon.glabot.annotations.Command
import net.dv8tion.jda.core.entities.Message

class CoreCog(botObj: Bot) extends Cog {
    override final val bot = botObj

    def getName(): String = {
        "Core"
    }

    def getDescription(): String = {
        "The core, essential cog to keep the bot running."
    }

    @Command(name = "ping2", description = "cmd desc", aliases = Array("alias_test1", "alias_test2"))
    def cmdPing2(msg: Message, args: Array[String]): Unit = {
        msg.getChannel.sendMessage("pong this is in progress").queue
    }
}