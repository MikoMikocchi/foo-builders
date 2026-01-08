package com.foobuilders.game.world

import com.foobuilders.game.world.blocks.BlockType

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
}
