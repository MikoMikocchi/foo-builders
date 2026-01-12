package com.foobuilders.world

import com.foobuilders.world.tiles.Materials
import org.junit.jupiter.api.Assertions.{assertEquals, assertTrue}
import org.junit.jupiter.api.Test

final class VoxelMapTest {
  private val defaultMaterial = Materials.Grass.id

  @Test
  def fillsAllLevels(): Unit = {
    val map = VoxelMap(chunkSize = 4, chunkRadius = 1, levels = 2, defaultMaterial = defaultMaterial)

    map.fill(defaultMaterial)

    map.foreachCellOnLevel(0) { (_, _, mat) =>
      assertEquals(defaultMaterial, mat)
    }
    map.foreachCellOnLevel(1) { (_, _, mat) =>
      assertEquals(defaultMaterial, mat)
    }
  }

  @Test
  def writesIntoChunksAndBounds(): Unit = {
    val map = VoxelMap(chunkSize = 4, chunkRadius = 1, levels = 2, defaultMaterial = defaultMaterial)

    map.setMaterial(cellX = -1, cellY = -1, level = 0, material = defaultMaterial)
    map.setMaterial(cellX = 3, cellY = 3, level = 1, material = defaultMaterial)

    assertTrue(map.inBounds(0, 0, 0))
    assertTrue(!map.inBounds(10, 0, 0))
  }

  @Test
  def storesColumnMetadata(): Unit = {
    val map = VoxelMap(chunkSize = 4, chunkRadius = 1, levels = 1, defaultMaterial = defaultMaterial)
    val meta = ColumnMetadata(height = 2.5f, moisture = 0.4f, temperature = 14.0f)

    map.updateColumnMetadata(0, 0, meta)

    val read = map.columnMetadataAt(0, 0)
    assertEquals(meta.height, read.height)
    assertEquals(meta.moisture, read.moisture)
    assertEquals(meta.temperature, read.temperature)
  }
}
