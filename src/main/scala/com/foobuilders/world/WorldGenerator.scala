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
  }
}
