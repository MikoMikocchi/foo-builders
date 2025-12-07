package dev.foobuilders.shared.math

final case class Vec3(x: Double, y: Double, z: Double) {
  def +(other: Vec3): Vec3 = Vec3(x + other.x, y + other.y, z + other.z)
  def -(other: Vec3): Vec3 = Vec3(x - other.x, y - other.y, z - other.z)
  def *(scalar: Double): Vec3 = Vec3(x * scalar, y * scalar, z * scalar)

  def magnitude(): Double = math.sqrt(x * x + y * y + z * z)

  /** Clamp the vector so it does not exceed the given length. */
  def clamp(maxLength: Double): Vec3 = {
    val length = magnitude()
    if (length <= maxLength || maxLength <= 0) this
    else this * (maxLength / length)
  }

  /** Convert Vec3 to Vec2 by dropping z coordinate. */
  def toVec2: Vec2 = Vec2(x, y)
}

object Vec3 {
  val Zero: Vec3 = Vec3(0d, 0d, 0d)

  /** Create Vec3 from Vec2 with z = 0. */
  def fromVec2(v: Vec2, z: Double = 0d): Vec3 = Vec3(v.x, v.y, z)
}
