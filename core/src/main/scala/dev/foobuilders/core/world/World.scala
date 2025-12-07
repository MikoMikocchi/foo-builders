package dev.foobuilders.core.world

enum BlockType {
  case Air
  case Solid
}

final class World private (
    private val blocks: Map[(Int, Int, Int), BlockType]
) {
  def isBlocked(x: Int, y: Int, z: Int): Boolean = {
    blocks.get((x, y, z)).contains(BlockType.Solid)
  }

  def getBlock(x: Int, y: Int, z: Int): BlockType = {
    blocks.getOrElse((x, y, z), BlockType.Air)
  }

  /** Get the highest solid block at (x, y), or -1 if none. */
  def getBlockHeight(x: Int, y: Int): Int = {
    (0 to 255).reverseIterator
      .find(z => isBlocked(x, y, z))
      .getOrElse(-1)
  }

  def setBlock(x: Int, y: Int, z: Int, blockType: BlockType): World = {
    val newBlocks = if (blockType == BlockType.Air) {
      blocks - ((x, y, z))
    } else {
      blocks + ((x, y, z) -> blockType)
    }
    new World(newBlocks)
  }

  /** Check if position (x, y, z) is valid for entity placement. */
  def canPlaceEntity(x: Double, y: Double, z: Double): Boolean = {
    val blockX = math.floor(x).toInt
    val blockY = math.floor(y).toInt
    val blockZ = math.floor(z).toInt

    // Check if the block at entity's position is air
    !isBlocked(blockX, blockY, blockZ) &&
    // Check if there's a solid block below (ground)
    isBlocked(blockX, blockY, blockZ - 1)
  }
}

object World {
  def empty: World = new World(Map.empty)

  def apply(blocks: Map[(Int, Int, Int), BlockType] = Map.empty): World =
    new World(blocks)
}
