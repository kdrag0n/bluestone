package com.khronodragon.glabot.cogs

import com.khronodragon.glabot.Bot

abstract class Cog {
    val name: String
    val description: String
    var bot: Bot

    def load(bot: Bot): Unit

    def unload(bot: Bot): Unit
}
