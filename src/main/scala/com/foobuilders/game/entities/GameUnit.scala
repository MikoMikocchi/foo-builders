package com.foobuilders.game.entities

import com.badlogic.gdx.math.Vector3

class GameUnit(val id: Int, var position: Vector3) {
  var selected: Boolean = false
  var hp: Float = 100f
  val maxHp: Float = 100f
  val speed: Float = 5f
  val radius: Float = 0.5f // For collision/selection

  // Made public for rendering debug paths
  val targetPosition: Vector3 = new Vector3(position)
  private val moveDirection: Vector3 = new Vector3()
  var isMoving: Boolean = false

  def update(delta: Float): Unit = {
    if (isMoving) {
      val distance = position.dst(targetPosition)
      if (distance < 0.1f) {
        position.set(targetPosition)
        isMoving = false
      } else {
        moveDirection.set(targetPosition).sub(position).nor()
        position.mulAdd(moveDirection, speed * delta)
      }
    }
  }

  def moveTo(target: Vector3): Unit = {
    // Keep Y aligned with current position for now (flat movement)
    // In a real game, you'd use pathfinding and ground clamping
    targetPosition.set(target.x, position.y, target.z)
    isMoving = true
  }

  def setSelected(selected: Boolean): Unit = {
    this.selected = selected
  }
}
