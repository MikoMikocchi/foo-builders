package dev.foobuilders.core.world

enum BlockType {
  case Air
  case Solid
}

final class World private (generator: ChunkGenerator) {
  private val chunks = scala.collection.mutable.Map.empty[ChunkPos, Chunk]

  def isBlocked(x: Int, y: Int, z: Int): Boolean = {
    getBlock(x, y, z) == BlockType.Solid
  }

  def getBlock(x: Int, y: Int, z: Int): BlockType = {
    if (z < 0 || z >= Chunk.SizeZ) BlockType.Air
    else {
      val (pos, localX, localY) = toChunkCoords(x, y)
      val chunk = ensureChunk(pos)
      chunk.blockAt(localX, localY, z)
    }
  }

  /** Get the highest solid block at (x, y), or -1 if none. */
  def getBlockHeight(x: Int, y: Int): Int = {
    val (pos, localX, localY) = toChunkCoords(x, y)
    val chunk = ensureChunk(pos)
    chunk.heightAt(localX, localY)
  }

  /** Mutates in place for performance but returns `this` for fluent building. */
  def setBlock(x: Int, y: Int, z: Int, blockType: BlockType): World = {
    if (z >= 0 && z < Chunk.SizeZ) {
      val (pos, localX, localY) = toChunkCoords(x, y)
      val chunk = ensureChunk(pos)
      chunk.setBlock(localX, localY, z, blockType)
    }
    this
  }

  /** Check if position (x, y, z) is valid for entity placement. */
  def canPlaceEntity(x: Double, y: Double, z: Double): Boolean = {
    val blockX = math.floor(x).toInt
    val blockY = math.floor(y).toInt
    val blockZ = math.floor(z).toInt

    !isBlocked(blockX, blockY, blockZ) &&
    isBlocked(blockX, blockY, blockZ - 1)
  }

  def loadedChunks: Iterable[(ChunkPos, Chunk)] = chunks

  def loadedChunkBounds: Option[WorldBounds] = {
    if (chunks.isEmpty) None
    else {
      val minCx = chunks.keysIterator.map(_.cx).min
      val maxCx = chunks.keysIterator.map(_.cx).max
      val minCy = chunks.keysIterator.map(_.cy).min
      val maxCy = chunks.keysIterator.map(_.cy).max
      Some(
        WorldBounds(
          minCx * Chunk.SizeX,
          (maxCx + 1) * Chunk.SizeX - 1,
          minCy * Chunk.SizeY,
          (maxCy + 1) * Chunk.SizeY - 1
        )
      )
    }
  }

  /** Pre-generate all chunks within a square radius around (centerX, centerY). */
  def preload(centerX: Int, centerY: Int, radiusChunks: Int): Unit = {
    val centerPos = ChunkPos(
      Math.floorDiv(centerX, Chunk.SizeX),
      Math.floorDiv(centerY, Chunk.SizeY)
    )
    for {
      dx <- -radiusChunks to radiusChunks
      dy <- -radiusChunks to radiusChunks
    } {
      ensureChunk(ChunkPos(centerPos.cx + dx, centerPos.cy + dy))
    }
  }

  private def ensureChunk(pos: ChunkPos): Chunk = {
    chunks.getOrElseUpdate(pos, generator.generate(pos))
  }

  private def toChunkCoords(
      x: Int,
      y: Int
  ): (ChunkPos, Int, Int) = {
    val cx = Math.floorDiv(x, Chunk.SizeX)
    val cy = Math.floorDiv(y, Chunk.SizeY)
    val localX = Math.floorMod(x, Chunk.SizeX)
    val localY = Math.floorMod(y, Chunk.SizeY)
    (ChunkPos(cx, cy), localX, localY)
  }
}

final case class WorldBounds(minX: Int, maxX: Int, minY: Int, maxY: Int)

object World {
  def empty: World = generated(_ => Chunk.empty)

  def generated(generator: ChunkGenerator): World =
    new World(generator)
}
