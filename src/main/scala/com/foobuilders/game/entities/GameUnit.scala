package com.foobuilders.game.entities

import com.badlogic.gdx.math.Vector3
import com.foobuilders.game.physics.{PhysicsBody, MovementForce}

class GameUnit(val id: Int, startPosition: Vector3) {
  // Components
  val physicsBody: PhysicsBody = new PhysicsBody(startPosition, mass = 1.0f)
  val movementForce: MovementForce =
    new MovementForce(new Vector3(), maxSpeed = 5f)

  // Game State
  var selected: Boolean = false
  var hp: Float = 100f
  val maxHp: Float = 100f

  // Delegates for convenience
  def position: Vector3 = physicsBody.position
  def radius: Float = physicsBody.radius

  // For compatibility with renderer
  def isMoving: Boolean = movementForce.active
  def targetPosition: Vector3 = movementForce.targetPosition

  def update(delta: Float): Unit = {
    // Apply specific behavior forces
    movementForce.apply(physicsBody, delta)
  }

  def moveTo(target: Vector3): Unit = {
    movementForce.setTarget(target)
  }

  def setSelected(selected: Boolean): Unit = {
    this.selected = selected
  }
}
