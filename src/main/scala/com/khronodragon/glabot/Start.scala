package com.khronodragon.glabot

object Start {
    println("Welcome to GLaBOT!")
    println("Performing self-test...")
    println("Java test returned: " + JavaTest.test())
    assert(JavaTest.test() == "It works!", "Java test FAILED!")
    println {
        """Self-test finished.
Now starting bot...
        """
    }
    val bot = new Bot
    bot start()
    println("Bot exited. Goodbye!")
    System exit 0
}
