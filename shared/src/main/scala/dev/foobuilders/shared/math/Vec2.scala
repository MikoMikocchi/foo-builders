package dev.foobuilders.shared.math

final case class Vec2(x: Double, y: Double) {
  def +(other: Vec2): Vec2 = Vec2(x + other.x, y + other.y)
  def -(other: Vec2): Vec2 = Vec2(x - other.x, y - other.y)
  def *(scalar: Double): Vec2 = Vec2(x * scalar, y * scalar)

  def magnitude(): Double = math.sqrt(x * x + y * y)

  /** Clamp the vector so it does not exceed the given length. */
  def clamp(maxLength: Double): Vec2 = {
    val length = magnitude()
    if (length <= maxLength || maxLength <= 0) this
    else this * (maxLength / length)
  }
}

object Vec2 {
  val Zero: Vec2 = Vec2(0d, 0d)
}
