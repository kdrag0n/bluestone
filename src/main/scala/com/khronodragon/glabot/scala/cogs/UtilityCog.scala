package com.khronodragon.bluestone.scala.cogs

import com.khronodragon.bluestone.{Bot, Context}
import com.khronodragon.bluestone.annotations.Command

class UtilityCog(botObj: Bot) extends Cog {
    override final val bot = botObj

    def getName(): String = "Utility"

    def getDescription(): String = "Utilities that we can't live without."
}
