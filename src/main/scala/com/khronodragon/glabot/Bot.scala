package com.khronodragon.glabot

import javax.script._
import java.util.concurrent._

import net.dv8tion.jda.core._
import net.dv8tion.jda.core.entities._
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.exceptions.RateLimitedException
import net.dv8tion.jda.core.hooks.ListenerAdapter
import net.dv8tion.jda.core.events._
import javax.security.auth.login.LoginException

import scala.collection.mutable.HashMap
import scala.reflect.runtime.universe._
import annotations.{Cog => CogAnnotation, Command => CommandAnnotation}
import cogs.{Cog, CoreCog}

class Bot extends ListenerAdapter with AbstractBot {
    private val executor = new ScheduledThreadPoolExecutor(1)
    private var tasks = Set[ScheduledFuture[_]]()
    var commands = HashMap[String, Command]()
    var cogs = HashMap[String, Cog]()
    var commandCalls = HashMap[String, Int]()

    def getShardNum(event: Event): Int = {
        event.getJDA.getShardInfo.getShardId + 1
    }

    override def onReady(event: ReadyEvent): Unit = {
        val jda = event.getJDA
        this.jda = jda
        val uid = jda.getSelfUser().getId
        println(s"[Shard ${getShardNum(event)}] Ready - UID: $uid")
        val task = new Runnable {
            def run() = {
                val statusLine =
                ThreadLocalRandom.current.nextInt(1, 12) match {
                    case 1 => s"with ${jda.getUsers.size} members"
                    case 2 => s"in ${jda.getTextChannels.size + jda.getVoiceChannels.size} channels"
                    case 3 => s"in ${jda.getGuilds.size} servers"
                    case 4 => s"in ${jda.getGuilds.size} guilds"
                    case 5 => s"from shard ${jda.getShardInfo.getShardId + 1} of ${jda.getShardInfo.getShardTotal}"
                    case 6 => "with my buddies"
                    case 7 => "with bits and bytes"
                    case 8 => "World Domination"
                    case 9 => "with you"
                    case 10 => "with potatoes"
                    case 11 => "something"
                    case _ => "severe ERROR!"
                }
                jda.getPresence.setGame(Game.of(statusLine))
            }
        }
        // scheduleAtFixedRate(runnable, initial delay, interval / period, time unit)
        val future = executor.scheduleAtFixedRate(task, 10, 120, TimeUnit SECONDS)
        tasks += future
    }

    override def onResume(event: ResumedEvent): Unit = {
        println(s"[Shard ${getShardNum(event)}] WebSocket resumed")
    }

    override def onReconnect(event: ReconnectedEvent): Unit = {
        println(s"[Shard ${getShardNum(event)}] Reconnected")
    }

    override def onShutdown(event: ShutdownEvent): Unit = {
        println(s"[Shard ${getShardNum(event)}] Finished shutting down")
    }

    override def onMessageReceived(event: MessageReceivedEvent): Unit = {
        val jda = event.getJDA
        val author = event.getAuthor

        if (author isBot)
            return
        if (author.getId == jda.getSelfUser().getId)
            return

        val prefix = ")"
        val message = event.getMessage
        val content = message.getRawContent
        val channel = event.getChannel
        val responseNum = event.getResponseNumber

        if (content.startsWith(prefix)) {
            var args = content.split("\\s")
            val cmdName = args{0}.stripPrefix(prefix)
            args = args.drop(1)

            if (commands contains cmdName) {
                val command = commands(cmdName)
                command.invoke(this, event, args, prefix)
                if (commandCalls contains command.name) {
                    commandCalls + (command.name -> (commandCalls(command.name) + 1))
                } else {
                    commandCalls + (command.name -> 1)
                }
            }
        }
    }

    def registerCogClass(cog: Cog): Cog = {
        val properties = listCommands[typeOf[cog]]
        cog
    }

    def listCommands[Tag: TypeTag]: List[(TermSymbol, Annotation)] = {
        val fields = typeOf[Tag].members.collect {
            case s: TermSymbol => s
        }.filter(s => s.isVal || s.isVar)

        fields.flatMap {
            f => f.annotations.find(_.tree.tpe =:= typeOf[CommandAnnotation]).map((f, _))
        }.toList
    }
}

object Bot {
    @throws[LoginException]("if we fail to login")
    @throws[RateLimitedException]("if we get ratelimited")
    def start(token: String, shardCount: Int = 1,
              accountType: AccountType = AccountType.BOT): Unit = {
        println("Starting bot...")

        if (shardCount < 1) {
            println("There needs to be at least 1 shard, or how will the bot work?")
            System.exit(0)
        }

        for (shardId <- 0 until shardCount) {
            val jda = new JDABuilder(accountType)
                .setToken(token)
                .addEventListener(new Bot)
                .setAudioEnabled(true)
                .setAutoReconnect(true)
                .setWebSocketTimeout(120000) // 2 minutes
                .setBulkDeleteSplittingEnabled(false)
                .setStatus(OnlineStatus.ONLINE)
                .setGame(Game.of("something"))

            if (shardCount > 1) {
                jda.useSharding(shardId, shardCount)
            }

            jda.buildAsync
            Thread.sleep(500)
        }
    }
}