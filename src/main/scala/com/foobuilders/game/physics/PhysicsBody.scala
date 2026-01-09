package com.foobuilders.game.physics

import com.badlogic.gdx.math.Vector3

class PhysicsBody(val position: Vector3, var mass: Float = 1.0f) {
  val velocity: Vector3 = new Vector3(0, 0, 0)
  val acceleration: Vector3 = new Vector3(0, 0, 0)
  val accumulatedForce: Vector3 = new Vector3(0, 0, 0)
  
  // Dimensions for collision
  var radius: Float = 0.5f
  var height: Float = 2.0f // Standard human height approximation

  def applyForce(force: Vector3): Unit = {
    accumulatedForce.add(force)
  }

  def clearForces(): Unit = {
    accumulatedForce.setZero()
  }

  def integrate(delta: Float): Unit = {
    if (mass <= 0) return // Static body

    // a = F / m
    acceleration.set(accumulatedForce).scl(1f / mass)
    
    // v = v + a * dt
    velocity.mulAdd(acceleration, delta)
    
    // p = p + v * dt
    position.mulAdd(velocity, delta)
    
    // Clear forces for next frame
    clearForces()
  }
}

