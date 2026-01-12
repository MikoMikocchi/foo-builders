package com.foobuilders.world

import com.foobuilders.world.tiles.MaterialRegistry
import com.foobuilders.world.tiles.Materials
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

final class TileMapTest {
  @Test
  def fillsWholeGridWithGrass(): Unit = {
    val reg     = MaterialRegistry.default
    val tileMap = TileMap(gridHalfCells = 2, defaultMaterial = Materials.Grass.id)

    tileMap.fill(Materials.Grass.id)

    tileMap.foreachCell { (x, y, mat) =>
      assertEquals(Materials.Grass.id, mat, s"Expected grass at ($x,$y)")
    }

    assertEquals("Grass", reg.resolve(Materials.Grass.id).displayName)
  }
}
