package com.khronodragon.bluestone

import net.dv8tion.jda.core.AccountType
import play.api.libs.json.Json

object Start extends App {
    println("Welcome to Bluestone!")
    assert(JavaTest.test() == "It works!", "Self-test: Java test FAILED! Something is very wrong with this build.")
    val handle = scala.io.Source.fromFile("auth.json")
    val rawJson = try handle.mkString finally handle.close()
    val auth = Json.parse(rawJson)
    val token = (auth \ "token").as[String]
    val shardCount = (auth \ "shardCount").asOpt[Int].getOrElse(1)

    val textAccountType = (auth \ "type").asOpt[String].getOrElse("bot")
    val accountType =
    textAccountType match {
        case "user" => AccountType.CLIENT
        case "bot" => AccountType.BOT
        case _ => println("Warning: unrecognized account type! Use either 'client' (user) or 'bot' (bot). Assuming bot.")
            AccountType.BOT

    }

    Bot.start(token = token,
              shardCount = shardCount,
              accountType = accountType)
}
