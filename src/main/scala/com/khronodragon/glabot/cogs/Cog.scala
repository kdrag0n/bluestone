package com.khronodragon.glabot.cogs

import com.khronodragon.glabot.{Bot, Command => CommandClass}
import com.khronodragon.glabot.annotations.Command

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

    def registerFor(bot: Bot): Unit = {
        val mirror = ru.runtimeMirror(getClass.getClassLoader)
        val properties = listCommands[this.type]
        println("propList: " + properties)
        for ((symbol, annotationOpt) <- properties) {
            if (annotationOpt.getOrElse(1) != 1) {
                val annotation = annotationOpt.get
                println("ANNO ARGS: " + annotation.tree.children.tail)
                val func = mirror.reflect(this).reflectMethod(symbol.asMethod)
                val args = annotation.tree.children.tail
                val command = new CommandClass(cmdName = args{0}.toString, cmdDesc = args{1}.toString,
                    cmdUsage = args{2}.toString, cmdHidden = args{3}.asInstanceOf[Boolean],
                    cmdNPerms = args{4}.asInstanceOf[Array[String]],
                    cmdNoPm = args{5}.asInstanceOf[Boolean],
                    cmdAliases = args{6}.asInstanceOf[Array[String]],
                    cmdCall = func)

                bot.commands + (command.name -> command)
                for (al <- command.aliases) {
                    bot.commands + (al -> command)
                }
            }
        }
        bot.cogs + (this.getName -> this)
    }

    def listCommands[Tag: ru.TypeTag]: List[(ru.MethodSymbol, Option[ru.Annotation])] = {
        ru.typeOf[Tag].decls.collect {
            case m: ru.MethodSymbol => m
        }.filter(m => m.annotations.nonEmpty).map {
            m => (m, m.annotations.find(_.tree.tpe =:= ru.typeOf[Command]))
        }.toList
    }
}
