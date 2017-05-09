package com.khronodragon.glabot.cogs

import com.khronodragon.glabot.{Bot, Context}
import com.khronodragon.glabot.annotations.Command

class UtilityCog(botObj: Bot) extends Cog {
    override final val bot = botObj

    def getName(): String = {
        "Utility"
    }

    def getDescription(): String = {
        "Utilities that we can't live without."
    }
}