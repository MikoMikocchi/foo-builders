package com.foobuilders.game.world

import com.foobuilders.game.world.blocks.BlockType

import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.Ray
import com.foobuilders.game.world.blocks.BlockType
import scala.util.control.Breaks._

class GameWorld(val width: Int, val height: Int, val depth: Int) {

  private val blocks = Array.ofDim[BlockType](width, height, depth)

  // Initialize with Air
  for (x <- 0 until width; y <- 0 until height; z <- 0 until depth) {
    blocks(x)(y)(z) = BlockType.Air
  }

  def setBlock(x: Int, y: Int, z: Int, blockType: BlockType): Unit = {
    if (boundsCheck(x, y, z)) {
      blocks(x)(y)(z) = blockType
    }
  }

  def getBlock(x: Int, y: Int, z: Int): BlockType = {
    if (boundsCheck(x, y, z)) blocks(x)(y)(z) else BlockType.Air
  }

  private def boundsCheck(x: Int, y: Int, z: Int): Boolean = {
    x >= 0 && x < width && y >= 0 && y < height && z >= 0 && z < depth
  }

  def generatePlatform(): Unit = {
    // Generate 50x50x1 platform centered or starting at 0
    // Prompt says "platform of blocks size 50x50x1".
    // I will assume x and z are horizontal, y is vertical (standard LibGDX 3D).

    val platformWidth = 50
    val platformDepth = 50
    val platformHeight = 1

    val startX = (width - platformWidth) / 2
    val startZ = (depth - platformDepth) / 2
    // Place at y=0

    for (x <- 0 until platformWidth; z <- 0 until platformDepth) {
      setBlock(startX + x, 0, startZ + z, BlockType.Stone)
    }
  }

  // Simple Ray-Voxel intersection (Digital Differential Analyzer - DDA style)
  def raycast(
      ray: Ray,
      maxDistance: Float
  ): Option[(Int, Int, Int, BlockType)] = {
    var x = Math.floor(ray.origin.x).toInt
    var y = Math.floor(ray.origin.y).toInt
    var z = Math.floor(ray.origin.z).toInt

    val stepX = if (ray.direction.x > 0) 1 else -1
    val stepY = if (ray.direction.y > 0) 1 else -1
    val stepZ = if (ray.direction.z > 0) 1 else -1

    val tDeltaX =
      if (ray.direction.x == 0) Float.MaxValue
      else Math.abs(1f / ray.direction.x)
    val tDeltaY =
      if (ray.direction.y == 0) Float.MaxValue
      else Math.abs(1f / ray.direction.y)
    val tDeltaZ =
      if (ray.direction.z == 0) Float.MaxValue
      else Math.abs(1f / ray.direction.z)

    val distX =
      if (stepX > 0) (Math.floor(ray.origin.x) + 1 - ray.origin.x)
      else (ray.origin.x - Math.floor(ray.origin.x))
    val distY =
      if (stepY > 0) (Math.floor(ray.origin.y) + 1 - ray.origin.y)
      else (ray.origin.y - Math.floor(ray.origin.y))
    val distZ =
      if (stepZ > 0) (Math.floor(ray.origin.z) + 1 - ray.origin.z)
      else (ray.origin.z - Math.floor(ray.origin.z))

    var tMaxX =
      if (ray.direction.x == 0) Float.MaxValue else tDeltaX * distX.toFloat
    var tMaxY =
      if (ray.direction.y == 0) Float.MaxValue else tDeltaY * distY.toFloat
    var tMaxZ =
      if (ray.direction.z == 0) Float.MaxValue else tDeltaZ * distZ.toFloat

    var hit: Option[(Int, Int, Int, BlockType)] = None

    // Safety break
    var steps = 0
    val maxSteps = maxDistance * 2 // Heuristic

    breakable {
      while (steps < maxSteps) {
        if (boundsCheck(x, y, z)) {
          val block = blocks(x)(y)(z)
          if (block.isSolid) {
            hit = Some((x, y, z, block))
            break()
          }
        }

        if (tMaxX < tMaxY) {
          if (tMaxX < tMaxZ) {
            x += stepX
            tMaxX += tDeltaX
          } else {
            z += stepZ
            tMaxZ += tDeltaZ
          }
        } else {
          if (tMaxY < tMaxZ) {
            y += stepY
            tMaxY += tDeltaY
          } else {
            z += stepZ
            tMaxZ += tDeltaZ
          }
        }
        steps += 1
      }
    }

    hit
  }
}
