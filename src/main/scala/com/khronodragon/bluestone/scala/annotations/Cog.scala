package com.khronodragon.bluestone.annotations

import scala.annotation.StaticAnnotation

case class Cog(name: String, description: String,
                   hidden: Boolean = false) extends StaticAnnotation
