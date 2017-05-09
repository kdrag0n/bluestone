package com.khronodragon.glabot.cogs

import com.khronodragon.glabot.{Bot, Context}
import com.khronodragon.glabot.annotations.Command

class CoreCog(botObj: Bot) extends Cog {
    override final val bot = botObj

    def getName(): String = {
        "Core"
    }

    def getDescription(): String = {
        "The core, essential cog to keep the bot running."
    }

    @Command(name = "ping", description = "Ping, pong!", aliases = Array("alias_test1", "alias_test2"))
    def cmdPing(ctx: Context): Unit = {
        val msg = s"Pong! WebSockets: ${ctx.jda.getPing}ms"
        val beforeTime = System.currentTimeMillis
        ctx.reply(msg).queue(message1 => {
            message1.editMessage(msg + s", message: calculating...").queue(message2 => {
                val msgPing = (System.currentTimeMillis - beforeTime) / 2.0
                message2.editMessage(msg + s", message: ${msgPing}ms").queue
            })
        })
    }

    @Command("rnum", "Get the current response number.")
    def cmdRnum(ctx: Context): Unit = {
        ctx.reply(s"The current response number is ${ctx.responseNumber}.").queue
    }

    @Command("help", "Because we all need help.")
    def cmdHelp(ctx: Context): Unit = {
        ctx.reply {
            """
**GLaBOT by Dragon5232**

Commands:
  \u2022 help - Show this help.
  \u2022 ping - Ping, pong!
  \u2022 rnum - Get the current response number.

That's it for now.
Remember that this is a huge work in progress!
                    """.stripMargin
        }.queue
    }
}