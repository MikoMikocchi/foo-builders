package dev.foobuilders.core.world

import scala.collection.mutable

/** Chunk coordinates (16x16 columns). */
final case class ChunkPos(cx: Int, cy: Int) {
  def originX: Int = cx * Chunk.SizeX
  def originY: Int = cy * Chunk.SizeY
}

/** Data for a single chunk. Stores blocks and a column height map. */
final class Chunk private (
    private val blocks: Array[Byte],
    private val heightMap: Array[Short]
) {
  import Chunk.*

  def blockAt(localX: Int, localY: Int, z: Int): BlockType = {
    if (!inBounds(localX, localY, z)) BlockType.Air
    else fromByte(blocks(index(localX, localY, z)))
  }

  def heightAt(localX: Int, localY: Int): Int = {
    val h = heightMap(heightIndex(localX, localY)).toInt
    if (h < 0) -1 else h
  }

  def setBlock(localX: Int, localY: Int, z: Int, blockType: BlockType): Unit = {
    if (!inBounds(localX, localY, z)) return

    val idx = index(localX, localY, z)
    blocks(idx) = toByte(blockType)

    val heightIdx = heightIndex(localX, localY)
    val currentHeight = heightMap(heightIdx).toInt

    blockType match {
      case BlockType.Air =>
        if (z == currentHeight) {
          // Need to recompute column height by scanning downward.
          val newHeight = findColumnHeight(localX, localY, z - 1)
          heightMap(heightIdx) = newHeight.toShort
        }
      case BlockType.Solid =>
        if (z > currentHeight) {
          heightMap(heightIdx) = z.toShort
        }
    }
  }

  private def findColumnHeight(localX: Int, localY: Int, startZ: Int): Int = {
    var z = math.min(startZ, SizeZ - 1)
    while (z >= 0) {
      val idx = index(localX, localY, z)
      if (blocks(idx) == BlockSolid) return z
      z -= 1
    }
    -1
  }
}

object Chunk {
  val SizeX: Int = 16
  val SizeY: Int = 16
  val SizeZ: Int = 256
  val Volume: Int = SizeX * SizeY * SizeZ

  private val BlockAir: Byte = 0.toByte
  private val BlockSolid: Byte = 1.toByte

  private def index(localX: Int, localY: Int, z: Int): Int = {
    (z * SizeY + localY) * SizeX + localX
  }

  private def heightIndex(localX: Int, localY: Int): Int =
    localY * SizeX + localX

  private def inBounds(localX: Int, localY: Int, z: Int): Boolean = {
    localX >= 0 && localX < SizeX &&
    localY >= 0 && localY < SizeY &&
    z >= 0 && z < SizeZ
  }

  private def toByte(blockType: BlockType): Byte = blockType match {
    case BlockType.Air   => BlockAir
    case BlockType.Solid => BlockSolid
  }

  private def fromByte(value: Byte): BlockType = value match {
    case BlockSolid => BlockType.Solid
    case _          => BlockType.Air
  }

  def empty: Chunk = {
    new Chunk(
      Array.fill[Byte](Volume)(BlockAir),
      Array.fill[Short](SizeX * SizeY)(-1.toShort)
    )
  }

  def filledWith(
      columnHeights: Array[Int]
  ): Chunk = {
    val blocks = Array.fill[Byte](Volume)(BlockAir)
    val heightMap = Array.fill[Short](SizeX * SizeY)(-1.toShort)

    var idx = 0
    while (idx < columnHeights.length) {
      val height = math.min(SizeZ - 1, math.max(-1, columnHeights(idx)))
      heightMap(idx) = height.toShort
      if (height >= 0) {
        val localY = idx / SizeX
        val localX = idx - localY * SizeX
        var z = 0
        while (z <= height) {
          blocks(index(localX, localY, z)) = BlockSolid
          z += 1
        }
      }
      idx += 1
    }
    new Chunk(blocks, heightMap)
  }
}

/** Provides chunks when the world requests them. */
trait ChunkGenerator {
  def generate(pos: ChunkPos): Chunk
}

/** Smooth 2D heightfield generator backed by fractal value noise. */
final class TerrainGenerator(
    seed: Long,
    baseHeight: Double = 14d,
    amplitude: Double = 22d,
    octaves: Int = 5,
    lacunarity: Double = 2.1,
    gain: Double = 0.5,
    scale: Double = 64d
) extends ChunkGenerator {
  private val noise = new FractalNoise2D(seed, octaves, lacunarity, gain, scale)

  override def generate(pos: ChunkPos): Chunk = {
    val heights = Array.ofDim[Int](Chunk.SizeX * Chunk.SizeY)
    var idx = 0
    val originX = pos.originX
    val originY = pos.originY
    while (idx < heights.length) {
      val localY = idx / Chunk.SizeX
      val localX = idx - localY * Chunk.SizeX
      val worldX = originX + localX
      val worldY = originY + localY

      val h = baseHeight + noise.height(worldX.toDouble, worldY.toDouble) * amplitude
      val clamped = math.max(0, math.min(Chunk.SizeZ - 1, h.round.toInt))
      heights(idx) = clamped

      idx += 1
    }
    Chunk.filledWith(heights)
  }
}

/** Simple fractal value noise with smooth interpolation (continuous across chunks). */
final class FractalNoise2D(
    seed: Long,
    octaves: Int,
    lacunarity: Double,
    gain: Double,
    baseScale: Double
) {
  private val normalization: Double = {
    var amp = 1.0
    var sum = 0.0
    var i = 0
    while (i < octaves) {
      sum += amp
      amp *= gain
      i += 1
    }
    if (sum == 0) 1.0 else sum
  }

  def height(x: Double, y: Double): Double = {
    var freq = 1.0 / baseScale
    var amp = 1.0
    var total = 0.0
    var i = 0
    while (i < octaves) {
      total += valueNoise(x * freq, y * freq) * amp
      freq *= lacunarity
      amp *= gain
      i += 1
    }
    total / normalization
  }

  private def valueNoise(x: Double, y: Double): Double = {
    val x0 = math.floor(x).toInt
    val y0 = math.floor(y).toInt
    val x1 = x0 + 1
    val y1 = y0 + 1

    val sx = fade(x - x0)
    val sy = fade(y - y0)

    val n00 = hash(x0, y0)
    val n10 = hash(x1, y0)
    val n01 = hash(x0, y1)
    val n11 = hash(x1, y1)

    val ix0 = lerp(n00, n10, sx)
    val ix1 = lerp(n01, n11, sx)
    lerp(ix0, ix1, sy)
  }

  private def lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t

  // Smoothstep-like fade to avoid sharp transitions.
  private def fade(t: Double): Double = t * t * t * (t * (t * 6 - 15) + 10)

  private def hash(x: Int, y: Int): Double = {
    var h = seed ^ (x.toLong * 0x9E3779B97F4A7C15L) ^ (y.toLong * 0xC2B2AE3D27D4EB4FL)
    h ^= (h >>> 33)
    h *= 0xff51afd7ed558ccdL
    h ^= (h >>> 33)
    h *= 0xc4ceb9fe1a85ec53L
    h ^= (h >>> 33)
    // Map to [-1, 1]
    (h.toDouble / Long.MaxValue.toDouble) * 2.0 - 1.0
  }
}
