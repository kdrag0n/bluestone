package com.khronodragon.glabot

import javax.script.ScriptEngineManager
import java.util.concurrent._

import net.dv8tion.jda.core._
import net.dv8tion.jda.core.entities._
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.exceptions.RateLimitedException
import net.dv8tion.jda.core.hooks.ListenerAdapter
import javax.security.auth.login.LoginException

import net.dv8tion.jda.core.events.ReadyEvent


class Bot extends ListenerAdapter {
    private var replSessions = Set[String]()
    private val executor = new ScheduledThreadPoolExecutor(2)
    private var tasks = Set[ScheduledFuture[_]]()

    override def onReady(event: ReadyEvent): Unit = {
        val jda = event.getJDA
        val uid = jda.getSelfUser().getId
        println(s"Shard ${jda.getShardInfo.getShardId + 1} ready | UID ${uid}")
        val task = new Runnable {
            def run() = {
                jda.getPresence.setGame(Game.of(""))
            }
        }
        // scheduleAtFixedRate(runnable, initial delay, interval / period, time unit)
        val future = executor.scheduleAtFixedRate(task, 1, 5, TimeUnit.SECONDS)
        tasks += future
    }

    override def onMessageReceived(event: MessageReceivedEvent): Unit = {
        val jda = event.getJDA
        val author = event.getAuthor()

        if (author isBot)
            return
        if (author.getId == jda.getSelfUser().getId)
            return

        val prefix = ")"
        val message = event.getMessage()
        val content = message.getRawContent()
        val channel = event.getChannel()
        val responseNum = event.getResponseNumber()

        if (content.startsWith(prefix)) {
            var args = content.split("\\s")
            val cmdName = args{0}.stripPrefix(prefix)
            args = args.drop(1)

            if (cmdName == "ping") {
                val msg = s"Pong! WebSockets: ${jda.getPing}ms"
                val beforeTime = System.currentTimeMillis
                channel.sendMessage(msg).queue(message1 => {
                    message1.editMessage(msg + s", message: calculating...").queue(message2 => {
                        val msgPing = System.currentTimeMillis - beforeTime
                        message2.editMessage(msg + s", message: ${msgPing}ms")
                    })
                })
            } else if (cmdName == "rnum") {
                channel.sendMessage(s"The current response number is ${responseNum}.").queue
            } else if (cmdName == "help") {
                channel.sendMessage {
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
            } else if (cmdName == "repl") {
                if (args.length < 1) {
                    channel.sendMessage("You need to specify a language, like `scala` or `js`!").queue
                    return
                }
                val language = args{0}
                if (replSessions contains channel.getId) {
                    channel.sendMessage("Already running a REPL session in this channel. Exit it with `quit`.").queue
                    return
                }
                replSessions += channel.getId

                val engine = new ScriptEngineManager().getEngineByName(language)
                engine.put("jda", jda)
                engine.put("message", message)
                engine.put("content", content)
                engine.put("author", author)
                engine.put("channel", channel)
                engine.put("guild", message.getGuild)
                engine.put("server", message.getGuild)
                engine.put("test", "Test right back at ya!")
                engine.put("msg", message)

                channel.sendMessage(s"Enter code to execute or evaluate. `exit()` or `quit` to exit. Prefix is: ```${prefix}```").queue
                while (true) {

                }
            }
        } else if (replSessions contains channel.getId) {
            val ownerId = "160567046642335746"
            if (author.getId == ownerId) {
                if (content.startsWith("`")) {
                    var code = content.replaceFirst("```scala", "").replaceFirst("```js", "").replaceFirst("```javascript", "").stripPrefix("`").stripSuffix("`")
                }
            }
        }
    }
}

object Bot {
    @throws[LoginException]("if we fail to login")
    @throws[RateLimitedException]("if we get ratelimited")
    def start(token: String, shardCount: Int = 1,
              accountType: AccountType = AccountType.BOT): Unit = {
        println("Starting bot...")
        for (shardId <- 0 until shardCount) {
            new JDABuilder(accountType)
                .setToken(token)
                .useSharding(shardId, shardCount)
                .addEventListener(new Bot)
                .setAudioEnabled(true)
                .setAutoReconnect(true)
                .setWebSocketTimeout(120000) // 2 minutes
                .setBulkDeleteSplittingEnabled(false)
                .setStatus(OnlineStatus.ONLINE)
                .setGame(Game.of(s"on shard ${shardId + 1} of ${shardCount}"))
                .buildAsync
            Thread.sleep(100)
        }
    }
}