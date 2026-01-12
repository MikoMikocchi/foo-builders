package com.foobuilders.screens.world

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.foobuilders.world.TileMap
import com.foobuilders.world.tiles.MaterialRegistry

final class TileRenderer(
    cellSize: Float,
    tileMap: TileMap,
    materials: MaterialRegistry
) {
  def render(shapes: ShapeRenderer): Unit = {
    shapes.begin(ShapeRenderer.ShapeType.Filled)

    tileMap.foreachCell { (cellX, cellY, materialId) =>
      val defn = materials.resolve(materialId)
      shapes.setColor(defn.color)
      shapes.rect(cellX * cellSize, cellY * cellSize, cellSize, cellSize)
    }

    shapes.end()
  }
}

object TileRenderer {
  def apply(cellSize: Float, tileMap: TileMap, materials: MaterialRegistry): TileRenderer =
    new TileRenderer(cellSize = cellSize, tileMap = tileMap, materials = materials)
}
