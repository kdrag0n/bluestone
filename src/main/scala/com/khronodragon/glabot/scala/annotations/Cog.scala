package com.khronodragon.glabot.annotations

import scala.annotation.StaticAnnotation

case class Cog(name: String, description: String,
                   hidden: Boolean = false) extends StaticAnnotation