package com.foobuilders.screens.world

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.foobuilders.world.VoxelMap
import com.foobuilders.world.tiles.MaterialRegistry

final class TileRenderer(
    cellSize: Float,
    map: VoxelMap,
    materials: MaterialRegistry
) {
  def render(shapes: ShapeRenderer, level: Int): Unit = {
    shapes.begin(ShapeRenderer.ShapeType.Filled)

    map.foreachCellOnLevel(level) { (cellX, cellY, materialId) =>
      val defn = materials.resolve(materialId)
      shapes.setColor(defn.color)
      shapes.rect(cellX * cellSize, cellY * cellSize, cellSize, cellSize)
    }

    shapes.end()
  }
}

object TileRenderer {
  def apply(cellSize: Float, map: VoxelMap, materials: MaterialRegistry): TileRenderer =
    new TileRenderer(cellSize = cellSize, map = map, materials = materials)
}
