package com.foobuilders.game.physics

import com.badlogic.gdx.math.Vector3

class MovementForce(val targetPosition: Vector3, val maxSpeed: Float = 5f, val acceleration: Float = 20f) extends Force {
  private val temp = new Vector3()
  var active: Boolean = false

  def setTarget(target: Vector3): Unit = {
    targetPosition.set(target)
    active = true
  }

  override def apply(body: PhysicsBody, delta: Float): Unit = {
    if (!active) {
        // Damping when stopping? Handled by DragForce
        return
    }

    // Ignore Y distance for flat movement if we want ground-based movement
    // But for general physics, we seek in 3D.
    // However, units usually walk on ground.
    // Let's do 2D seek on XZ plane and let gravity handle Y, 
    // unless it's a flying unit. Assuming walking units.
    
    val dx = targetPosition.x - body.position.x
    val dz = targetPosition.z - body.position.z
    val dist = Math.sqrt(dx * dx + dz * dz).toFloat
    
    if (dist < 0.1f) {
        active = false
        // Snap to target? Or just stop applying force.
        // Stop x/z velocity?
        body.velocity.x = 0
        body.velocity.z = 0
        return
    }

    // Desired velocity
    temp.set(dx, 0, dz).nor().scl(maxSpeed)
    
    // Steering force: F = (Desired - Velocity) * coeff
    // Or just add acceleration in direction
    // Simple approach: Accelerate towards desired velocity
    
    // Diff between desired and current (on XZ)
    val vx = body.velocity.x
    val vz = body.velocity.z
    
    // Steering
    val steerX = temp.x - vx
    val steerZ = temp.z - vz
    
    // Clamp steering to acceleration capability
    // We want F = ma. So force = mass * acceleration.
    // Let's just apply force in direction of steering
    
    temp.set(steerX, 0, steerZ)
    if (temp.len2() > 0.001f) {
        temp.nor().scl(acceleration * body.mass)
        body.applyForce(temp)
    }
  }
}

