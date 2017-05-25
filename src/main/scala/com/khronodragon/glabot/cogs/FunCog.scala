package com.khronodragon.glabot.cogs

import com.khronodragon.glabot.{Bot, Context}
import com.khronodragon.glabot.annotations.Command

class FunCog(botObj: Bot) extends Cog {
    override final val bot = botObj

    def getName(): String = "Fun"

    def getDescription(): String = "The core, essential cog to keep the bot running."
}