package com.khronodragon.glabot.cogs

import com.khronodragon.glabot.Bot
import com.khronodragon.glabot.annotations.{Command, Cog => CogInfo}
import net.dv8tion.jda.core.entities.Message

@CogInfo(name = "Core", description = "Essentials")
class CoreCog extends Cog {
    override final val name = "Core"
    override final val description = "The core, essential cog to keep the bot running."

    def load(bot: Bot): Unit = {
        this.bot = bot
    }

    def unload(bot: Bot): Unit = {
        true
    }

    @Command(name = "ping2", description = "cmd desc")
    def cmdPing2(msg: Message, args: Array[String]): Unit = {
        msg.getChannel.sendMessage("pong this is in progress").queue
    }
}