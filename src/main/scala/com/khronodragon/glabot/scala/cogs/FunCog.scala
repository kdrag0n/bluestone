package com.khronodragon.bluestone.scala.cogs

import com.khronodragon.bluestone.{Bot, Context}
import com.khronodragon.bluestone.annotations.Command

class FunCog(botObj: Bot) extends Cog {
    override final val bot = botObj

    def getName(): String = "Fun"

    def getDescription(): String = "The core, essential cog to keep the bot running."
}
