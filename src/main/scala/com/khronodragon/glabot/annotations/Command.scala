package com.khronodragon.glabot.annotations

import scala.annotation.StaticAnnotation

case class Command(name: String, description: String,
                  usage: String = "", hidden: Boolean = false,
                  perms: Array[String] = Array[String](), noPm: Boolean = false,
                  aliases: Array[String] = Array[String]()) extends StaticAnnotation