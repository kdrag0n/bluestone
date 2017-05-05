package com.khronodragon.glabot

import play.api.libs.json.Json

object Start extends App {
    println("Welcome to GLaBOT!")
    println("Performing self-test...")
    println("Java test returned: " + JavaTest.test())
    assert(JavaTest.test() == "It works!", "Java test FAILED!")
    println {
        """Self-test finished.
Now starting bot...
        """
    }
    val handle = scala.io.Source.fromFile("auth.json")
    val rawJson = try handle.mkString finally handle.close()
    val auth = Json.parse(rawJson)
    Bot.start((auth \ "token").as[String])
    println("Bot exited. Goodbye!")
    System exit 0
}
