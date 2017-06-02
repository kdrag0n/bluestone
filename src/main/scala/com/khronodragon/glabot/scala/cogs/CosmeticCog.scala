package com.khronodragon.bluestone.scala.cogs

import com.khronodragon.bluestone.{Bot, Context}
import com.khronodragon.bluestone.annotations.Command

class CosmeticCog(botObj: Bot) extends Cog {
    override final val bot = botObj

    def getName(): String = "Cosmetic"

    def getDescription(): String = "Some nice cosmetic stuff for the bot to have."
}
