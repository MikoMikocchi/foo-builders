package com.foobuilders.world

import com.foobuilders.world.tiles.{MaterialId, Materials}

object WorldGenerator {
  def flatGround(map: VoxelMap, groundMaterial: MaterialId, surfaceLevel: Int): Unit = {
    val clampedSurface = surfaceLevel.max(0).min(map.depth - 1)

    // Clear to air first
    map.fill(Materials.Air.id)

    // Underground up to surface-1
    var lvl = 0
    while (lvl < clampedSurface) {
      map.fillLevel(lvl, Materials.Dirt.id)
      lvl += 1
    }

    // Surface top
    map.fillLevel(clampedSurface, groundMaterial)

    map.foreachCellOnLevel(clampedSurface) { (cellX, cellY, _) =>
      map.updateColumnMetadata(
        cellX,
        cellY,
        ColumnMetadata(height = clampedSurface.toFloat, moisture = 0.0f, temperature = 0.0f)
      )
    }
  }

  /** Generate gently varying hills using value-noise-based fBm. The output stays close to `baseSurfaceLevel` with small
    * smooth offsets.
    */
  def gentleHills(
      map: VoxelMap,
      surfaceMaterial: MaterialId,
      baseSurfaceLevel: Int,
      amplitude: Int = 4,
      frequency: Float = 0.06f,
      octaves: Int = 3,
      seed: Long = 1337L,
      subSurfaceMaterial: MaterialId = Materials.Dirt.id
  ): Unit = {
    val clampedBase = baseSurfaceLevel.max(0).min(map.depth - 1)
    val maxLevel    = map.depth - 1

    map.fill(Materials.Air.id)

    var y = -map.halfCells
    while (y < map.halfCells) {
      var x = -map.halfCells
      while (x < map.halfCells) {
        val heightOffset = math.round(fbm(x * frequency, y * frequency, octaves, seed) * amplitude.toFloat)
        val surfaceLevel = (clampedBase + heightOffset).max(0).min(maxLevel)

        fillColumn(map, x, y, surfaceLevel, surfaceMaterial, subSurfaceMaterial)
        map.updateColumnMetadata(
          x,
          y,
          ColumnMetadata(height = surfaceLevel.toFloat, moisture = 0.0f, temperature = 0.0f)
        )

        x += 1
      }
      y += 1
    }
  }

  private def fillColumn(
      map: VoxelMap,
      cellX: Int,
      cellY: Int,
      surfaceLevel: Int,
      surfaceMaterial: MaterialId,
      subSurfaceMaterial: MaterialId
  ): Unit = {
    var lvl = 0
    while (lvl < surfaceLevel) {
      map.setMaterial(cellX, cellY, lvl, subSurfaceMaterial)
      lvl += 1
    }

    map.setMaterial(cellX, cellY, surfaceLevel, surfaceMaterial)
  }

  private def fbm(x: Float, y: Float, octaves: Int, seed: Long): Float = {
    var freq   = 1.0f
    var amp    = 1.0f
    var sum    = 0.0f
    var norm   = 0.0f
    var octave = 0

    while (octave < octaves.max(1)) {
      sum += valueNoise(x * freq, y * freq, seed) * amp
      norm += amp
      amp *= 0.55f
      freq *= 2.0f
      octave += 1
    }

    if (norm == 0.0f) 0.0f else (sum / norm).max(-1.0f).min(1.0f)
  }

  private def valueNoise(x: Float, y: Float, seed: Long): Float = {
    val x0 = math.floor(x.toDouble).toInt
    val y0 = math.floor(y.toDouble).toInt
    val x1 = x0 + 1
    val y1 = y0 + 1

    val sx = x - x0
    val sy = y - y0

    val n00 = hashNoise(x0, y0, seed)
    val n10 = hashNoise(x1, y0, seed)
    val n01 = hashNoise(x0, y1, seed)
    val n11 = hashNoise(x1, y1, seed)

    val ix0 = lerp(n00, n10, sx)
    val ix1 = lerp(n01, n11, sx)
    lerp(ix0, ix1, sy)
  }

  private def hashNoise(x: Int, y: Int, seed: Long): Float = {
    var h = seed ^ (x.toLong * 0x27d4eb2dL) ^ (y.toLong * 0x165667b1L)
    h ^= (h >> 15)
    h *= 0x85ebca6bL
    h ^= (h >> 13)

    val normalized = (h & 0xffffffffL).toDouble / 0xffffffffL.toDouble
    (normalized * 2.0 - 1.0).toFloat
  }

  private def lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}
