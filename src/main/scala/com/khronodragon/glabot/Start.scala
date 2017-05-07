package com.khronodragon.glabot

import net.dv8tion.jda.core.AccountType
import play.api.libs.json.Json

object Start extends App {
    println("Welcome to GLaBOT!")
    assert(JavaTest.test() == "It works!", "Self-test: Java test FAILED!")
    val handle = scala.io.Source.fromFile("auth.json")
    val rawJson = try handle.mkString finally handle.close()
    val auth = Json.parse(rawJson)
    val token = (auth \ "token").as[String]
    val shardCount = (auth \ "shardCount").asOpt[Int].getOrElse(1)

    val textAccountType = (auth \ "type").asOpt[String].getOrElse("bot")
    var accountType = AccountType
    textAccountType match {
        case "user" => accountType = AccountType.CLIENT
        case "bot" => accountType = AccountType.BOT
        case _ => accountType = AccountType.BOT
            println("Warning: unrecognized account type! Use either 'client' (user) or 'bot' (bot). Assuming bot.")
    }

    Bot.start(token = token,
              shardCount = shardCount,
              accountType = accountType)
}
