package com.khronodragon.glabot.cogs

import com.khronodragon.glabot.Bot

abstract class Cog {
    def getName(): String
    def getDescription(): String
    val bot: Bot

    def load(): Unit = {
        println(s"[$getName] Cog loaded.")
    }

    def unload(): Unit = {
        println(s"[$getName] Cog unloaded.")
    }
}
