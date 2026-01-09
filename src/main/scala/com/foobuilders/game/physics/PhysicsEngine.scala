package com.foobuilders.game.physics

import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.foobuilders.game.world.blocks.BlockType
import scala.collection.mutable.ArrayBuffer

trait WorldProvider {
  def getBlock(x: Int, y: Int, z: Int): BlockType
}

class PhysicsEngine(val worldProvider: WorldProvider) {
  val forces = ArrayBuffer[Force]()
  val bodies = ArrayBuffer[PhysicsBody]()

  // Global settings
  val gravity = new GravityForce()
  val drag = new DragForce(0.5f) // Increased drag for stability

  def addBody(body: PhysicsBody): Unit = {
    bodies += body
  }

  def removeBody(body: PhysicsBody): Unit = {
    bodies -= body
  }

  def update(delta: Float): Unit = {
    bodies.foreach { body =>
      // Apply global forces
      gravity.apply(body, delta)
      drag.apply(body, delta)
      
      // Integrate
      body.integrate(delta)
      
      // Resolve collisions
      resolveBlockCollisions(body)
    }
    
    resolveBodyCollisions()
  }

  private def resolveBlockCollisions(body: PhysicsBody): Unit = {
    // AABB collision detection
    // Body AABB
    val minX = body.position.x - body.radius
    val minY = body.position.y
    val minZ = body.position.z - body.radius
    val maxX = body.position.x + body.radius
    val maxY = body.position.y + body.height
    val maxZ = body.position.z + body.radius

    // Check blocks in range
    val startX = Math.floor(minX).toInt
    val endX = Math.floor(maxX).toInt
    val startY = Math.floor(minY).toInt
    val endY = Math.floor(maxY).toInt
    val startZ = Math.floor(minZ).toInt
    val endZ = Math.floor(maxZ).toInt

    for (y <- startY to endY) {
      for (x <- startX to endX) {
        for (z <- startZ to endZ) {
          val block = worldProvider.getBlock(x, y, z)
          if (block != null && block.isSolid) {
            resolveSingleBlockCollision(body, x, y, z)
          }
        }
      }
    }
    
    // Check ground plane (y = 0) implicitly if no blocks? 
    // Or just let them fall if no blocks.
    // Assuming blocks start at y=0.
  }

  private def resolveSingleBlockCollision(body: PhysicsBody, bx: Int, by: Int, bz: Int): Unit = {
    // Simple AABB separation
    // Calculate overlap on each axis
    // Block AABB
    val bMinX = bx.toFloat
    val bMaxX = bx + 1f
    val bMinY = by.toFloat
    val bMaxY = by + 1f
    val bMinZ = bz.toFloat
    val bMaxZ = bz + 1f

    // Body AABB
    val pMinX = body.position.x - body.radius
    val pMaxX = body.position.x + body.radius
    val pMinY = body.position.y
    val pMaxY = body.position.y + body.height
    val pMinZ = body.position.z - body.radius
    val pMaxZ = body.position.z + body.radius

    // Check overlaps
    val overlapX = Math.min(pMaxX, bMaxX) - Math.max(pMinX, bMinX)
    val overlapY = Math.min(pMaxY, bMaxY) - Math.max(pMinY, bMinY)
    val overlapZ = Math.min(pMaxZ, bMaxZ) - Math.max(pMinZ, bMinZ)

    if (overlapX > 0 && overlapY > 0 && overlapZ > 0) {
      // Find minimum overlap to push out
      // Prefer pushing vertically (Y) if overlaps are close, or if falling
      
      // Simple logic: push out on axis with min overlap
      if (overlapY < overlapX && overlapY < overlapZ) {
        // Vertical collision
        if (pMinY < bMinY) {
           // Hitting from bottom (head bump)
           body.position.y = bMinY - body.height
           if (body.velocity.y > 0) body.velocity.y = 0
        } else {
           // Landing on top
           body.position.y = bMaxY
           if (body.velocity.y < 0) body.velocity.y = 0
           // Ground friction could be applied here
        }
      } else if (overlapX < overlapZ) {
        // X collision
        if (body.position.x < bx + 0.5f) {
           body.position.x = bMinX - body.radius
        } else {
           body.position.x = bMaxX + body.radius
        }
        body.velocity.x = 0
      } else {
        // Z collision
        if (body.position.z < bz + 0.5f) {
           body.position.z = bMinZ - body.radius
        } else {
           body.position.z = bMaxZ + body.radius
        }
        body.velocity.z = 0
      }
    }
  }

  private def resolveBodyCollisions(): Unit = {
    for (i <- 0 until bodies.length) {
      val b1 = bodies(i)
      for (j <- i + 1 until bodies.length) {
        val b2 = bodies(j)
        
        val dist = b1.position.dst(b2.position)
        val minDist = b1.radius + b2.radius
        
        if (dist < minDist && dist > 0.0001f) {
          // Push apart
          val overlap = minDist - dist
          val pushDir = b1.position.cpy().sub(b2.position).nor()
          
          // Split push based on mass (simplified: equal split)
          val pushVector = pushDir.scl(overlap * 0.5f)
          
          b1.position.add(pushVector)
          b2.position.sub(pushVector)
          
          // Simple elastic bounce (optional, or just kill velocity towards each other)
          // For now just positional correction
        }
      }
    }
  }
}

