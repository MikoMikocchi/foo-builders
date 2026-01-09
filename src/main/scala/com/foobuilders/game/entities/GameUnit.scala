package com.foobuilders.game.entities

import com.badlogic.gdx.math.Vector3
import com.foobuilders.game.physics.{PhysicsBody, MovementForce}

class GameUnit(val id: Int, val kind: UnitKind, startPosition: Vector3) {
  private val stats = kind.stats

  // Components
  val physicsBody: PhysicsBody = new PhysicsBody(startPosition, mass = stats.mass)
  physicsBody.radius = stats.radius
  physicsBody.height = stats.height

  val movementForce: MovementForce =
    new MovementForce(new Vector3(), maxSpeed = stats.maxSpeed, acceleration = stats.acceleration)

  // Game State
  var selected: Boolean = false
  var hp: Float = stats.maxHp
  val maxHp: Float = stats.maxHp

  // Delegates for convenience
  def position: Vector3 = physicsBody.position
  def radius: Float = physicsBody.radius
  def style: UnitStyle = kind.style

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
