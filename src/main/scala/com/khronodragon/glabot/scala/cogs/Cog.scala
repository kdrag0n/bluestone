package com.khronodragon.bluestone.scala.cogs

import com.khronodragon.bluestone.{Bot, Command => CommandClass}
import com.khronodragon.bluestone.annotations.Command

import scala.reflect.runtime.{universe => ru}

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

    def getType(): Class[_] = {
        this.getClass
    }

    @Command("name", "desc")
    def methodOnOrig(): Unit = {}

    def register(): Unit = {
        val classObj = this.getClass

        for (method <- classObj.getDeclaredMethods) {
            if (method.isAnnotationPresent(Command.class)) {
                val anno = method.getAnnotation[Class[_]](Command.class).asInstanceOf[Command]

                val command = new CommandClass(cmdName = anno.name(), cmdDesc = anno.desc(),
                    cmdUsage = anno.usage(), cmdHidden = anno.hidden())

                bot.commands + (command.name -> command)
                for (al <- command.aliases) {
                    bot.commands + (al -> command)
                }
            }
        }
        val command = new CommandClass(cmdName = args{0}.toString, cmdDesc = args{1}.toString,
            cmdUsage = args{2}.toString, cmdHidden = args{3}.asInstanceOf[Boolean],
            cmdNPerms = args{4}.asInstanceOf[Array[String]],
            cmdNoPm = args{5}.asInstanceOf[Boolean],
            cmdAliases = args{6}.asInstanceOf[Array[String]],
            cmdCall = func)
        bot.cogs + (this.getName -> this)
    }
