package com.khronodragon.glabot.scala.cogs

import com.khronodragon.glabot.{Bot, Context}
import com.khronodragon.glabot.annotations.Command

class CosmeticCog(botObj: Bot) extends Cog {
    override final val bot = botObj

    def getName(): String = "Cosmetic"

    def getDescription(): String = "Some nice cosmetic stuff for the bot to have."
}