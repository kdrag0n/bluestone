package com.khronodragon.glabot

import play.api.libs.json.Json

object Start extends App {
    println("Welcome to GLaBOT!")
    assert(JavaTest.test() == "It works!", "Self-test: Java test FAILED!")
    val handle = scala.io.Source.fromFile("auth.json")
    val rawJson = try handle.mkString finally handle.close()
    val auth = Json.parse(rawJson)
    val token = (auth \ "token").as[String]

    Bot.start(token = token,
              shardCount = shardCount,
              accountType = accountType)
}
