package com.foobuilders.game.physics

import com.badlogic.gdx.math.Vector3

trait Force {
  def apply(body: PhysicsBody, delta: Float): Unit
}

class GravityForce(gravity: Vector3 = new Vector3(0, -9.8f, 0)) extends Force {
  override def apply(body: PhysicsBody, delta: Float): Unit = {
    // F = m * g
    val force = new Vector3(gravity).scl(body.mass)
    body.applyForce(force)
  }
}

class DragForce(dragCoefficient: Float = 0.1f) extends Force {
  override def apply(body: PhysicsBody, delta: Float): Unit = {
    // F = -c * v
    val force = new Vector3(body.velocity).scl(-dragCoefficient)
    body.applyForce(force)
  }
}

