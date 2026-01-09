package com.foobuilders.game.entities

import com.badlogic.gdx.graphics.Color

case class UnitStats(
    maxHp: Float,
    mass: Float,
    maxSpeed: Float,
    acceleration: Float,
    radius: Float = 0.5f,
    height: Float = 2.0f
)

sealed trait UnitShape
object UnitShape {
  case object ConeInfantry extends UnitShape
  case object BoxWorker extends UnitShape
}

case class UnitStyle(primary: Color, secondary: Color, shape: UnitShape)

sealed trait UnitKind {
  def id: String
  def displayName: String
  def stats: UnitStats
  def style: UnitStyle
}

object UnitKinds {
  case object Soldier extends UnitKind {
    override val id: String = "soldier"
    override val displayName: String = "Солдат"
    override val stats: UnitStats = UnitStats(
      maxHp = 120f,
      mass = 1.2f,
      maxSpeed = 6.0f,
      acceleration = 24.0f,
      radius = 0.55f,
      height = 2.0f
    )
    override val style: UnitStyle = UnitStyle(
      primary = Color.valueOf("355C7D"), // deep steel blue
      secondary = Color.SKY,
      shape = UnitShape.ConeInfantry
    )
  }

  case object Builder extends UnitKind {
    override val id: String = "builder"
    override val displayName: String = "Строитель"
    override val stats: UnitStats = UnitStats(
      maxHp = 90f,
      mass = 1.0f,
      maxSpeed = 4.5f,
      acceleration = 18.0f,
      radius = 0.6f,
      height = 1.9f
    )
    override val style: UnitStyle = UnitStyle(
      primary = Color.valueOf("E07A5F"), // terracotta
      secondary = Color.valueOf("F2CC8F"), // sand accents
      shape = UnitShape.BoxWorker
    )
  }

  val all: Seq[UnitKind] = Seq(Soldier, Builder)
}
