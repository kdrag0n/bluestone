package com.khronodragon.glabot.scala.annotations

import scala.annotation.StaticAnnotation

case class ScalaCommand(name: String, description: String,
                  usage: String = "", hidden: Boolean = false,
                  perms: Array[String] = Array[String](), noPm: Boolean = false,
                  aliases: Array[String] = Array[String]()) extends StaticAnnotation