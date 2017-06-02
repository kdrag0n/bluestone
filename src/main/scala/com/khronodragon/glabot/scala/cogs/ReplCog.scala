package com.khronodragon.bluestone.cogs

import javax.script.ScriptEngineManager

import com.khronodragon.bluestone.{Bot, Context}
import com.khronodragon.bluestone.annotations.Command

class ReplCog(botObj: Bot) extends Cog {
    override final val bot = botObj
    private var replSessions = Set[Long]()

    def getName(): String = "REPL"

    def getDescription(): String = "A multilingual REPL, in Discord!"

    @Command(name = "repl", description = "A multilingual REPL, in Discord!")
    def cmdRepl(ctx: Context): Unit = {
        if (ctx.args.length < 1) {
            ctx.reply("You need to specify a language, like `scala` or `js`!").queue
            return
        }
        val language = ctx.args{0}
        println(language)
        if (replSessions contains ctx.channelIdLong) {
            ctx.reply("Already running a REPL session in this channel. Exit it with `quit`.").queue
            return
        }
        replSessions += ctx.channelIdLong

        val engine = new ScriptEngineManager().getEngineByName(language)
        engine.put("jda", ctx.jda)
        engine.put("message", ctx.message)
        engine.put("content", ctx.content)
        engine.put("author", ctx.author)
        engine.put("channel", ctx.channel)
        engine.put("guild", ctx.guild)
        engine.put("server", ctx.server)
        engine.put("test", "Test right back at ya!")
        engine.put("msg", ctx.message)

        ctx.reply(s"Enter code to execute or evaluate. `exit()` or `quit` to exit. Prefix is: ```${ctx.prefix}```").queue
        while (false) {

        }
    }
}

/*
else if (replSessions contains channel.getId) {
            val ownerId = "160567046642335746"
            if (author.getId == ownerId) {
                if (content.startsWith("`")) {
                    var code = content.replaceFirst("```scala", "").replaceFirst("```js", "").replaceFirst("```javascript", "").stripPrefix("`").stripSuffix("`")
                }
            }
        }
 */
