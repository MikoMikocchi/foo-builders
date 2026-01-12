package com.foobuilders.world

import com.foobuilders.world.tiles.MaterialId

final case class ColumnMetadata(height: Float, moisture: Float, temperature: Float)

private final class ChunkData(chunkSize: Int, levels: Int, defaultMaterial: MaterialId) {
  private val data: Array[MaterialId] = Array.fill(chunkSize * chunkSize * levels)(defaultMaterial)

  def fill(material: MaterialId): Unit = {
    var i = 0
    while (i < data.length) {
      data(i) = material
      i += 1
    }
  }

  def set(localX: Int, localY: Int, level: Int, material: MaterialId): Unit =
    data(index(localX, localY, level)) = material

  def materialAt(localX: Int, localY: Int, level: Int): MaterialId =
    data(index(localX, localY, level))

  private inline def index(localX: Int, localY: Int, level: Int): Int =
    (level * chunkSize + localY) * chunkSize + localX
}

final class VoxelMap(
    val chunkSize: Int,
    val chunkRadius: Int,
    val levels: Int,
    defaultMaterial: MaterialId
) {
  private val chunkDiameter = chunkRadius * 2

  /** Number of cells from origin to edge along X/Y. */
  val halfCells: Int = chunkRadius * chunkSize
  val width: Int     = chunkDiameter * chunkSize
  val height: Int    = width // square map for now
  val depth: Int     = levels

  private val chunks: Array[ChunkData] =
    Array.fill(chunkDiameter * chunkDiameter)(ChunkData(chunkSize, levels, defaultMaterial))

  private val columnMetadata: Array[ColumnMetadata] =
    Array.fill(width * height)(ColumnMetadata(height = 0.0f, moisture = 0.0f, temperature = 0.0f))

  def fill(material: MaterialId): Unit =
    chunks.foreach(_.fill(material))

  def inBounds(cellX: Int, cellY: Int, level: Int): Boolean =
    cellX >= -halfCells && cellX < halfCells &&
      cellY >= -halfCells && cellY < halfCells &&
      level >= 0 && level < levels

  def setMaterial(cellX: Int, cellY: Int, level: Int, material: MaterialId): Unit = {
    if (!inBounds(cellX, cellY, level)) return
    val (chunk, localX, localY) = resolveChunk(cellX, cellY)
    chunk.set(localX, localY, level, material)
  }

  def materialAt(cellX: Int, cellY: Int, level: Int): MaterialId = {
    if (!inBounds(cellX, cellY, level)) return defaultMaterial
    val (chunk, localX, localY) = resolveChunk(cellX, cellY)
    chunk.materialAt(localX, localY, level)
  }

  def foreachCellOnLevel(level: Int)(f: (Int, Int, MaterialId) => Unit): Unit = {
    if (level < 0 || level >= levels) return

    var iy = 0
    while (iy < height) {
      val cellY = iy - halfCells

      var ix = 0
      while (ix < width) {
        val cellX = ix - halfCells
        val mat   = materialAt(cellX, cellY, level)
        f(cellX, cellY, mat)

        ix += 1
      }

      iy += 1
    }
  }

  def updateColumnMetadata(cellX: Int, cellY: Int, meta: ColumnMetadata): Unit = {
    if (!inBounds(cellX, cellY, level = 0)) return
    columnMetadata(columnIndex(cellX, cellY)) = meta
  }

  def columnMetadataAt(cellX: Int, cellY: Int): ColumnMetadata = {
    if (!inBounds(cellX, cellY, level = 0)) return ColumnMetadata(0.0f, 0.0f, 0.0f)
    columnMetadata(columnIndex(cellX, cellY))
  }

  private def resolveChunk(cellX: Int, cellY: Int): (ChunkData, Int, Int) = {
    val chunkX = Math.floorDiv(cellX, chunkSize)
    val chunkY = Math.floorDiv(cellY, chunkSize)
    val idx    = chunkIndex(chunkX, chunkY)

    val localX = Math.floorMod(cellX, chunkSize)
    val localY = Math.floorMod(cellY, chunkSize)

    (chunks(idx), localX, localY)
  }

  private def chunkIndex(chunkX: Int, chunkY: Int): Int = {
    val ix = chunkX + chunkRadius
    val iy = chunkY + chunkRadius
    iy * chunkDiameter + ix
  }

  private def columnIndex(cellX: Int, cellY: Int): Int = {
    val ix = cellX + halfCells
    val iy = cellY + halfCells
    iy * width + ix
  }
}

object VoxelMap {
  def apply(chunkSize: Int, chunkRadius: Int, levels: Int, defaultMaterial: MaterialId): VoxelMap =
    new VoxelMap(chunkSize = chunkSize, chunkRadius = chunkRadius, levels = levels, defaultMaterial = defaultMaterial)
}
