package com.foobuilders.world

import com.foobuilders.world.tiles.MaterialId

object WorldGenerator {
  def flatGround(map: VoxelMap, groundMaterial: MaterialId): Unit = {
    // Fill all levels with the same material for now.
    map.fill(groundMaterial)
  }
}
