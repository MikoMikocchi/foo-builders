package com.foobuilders.screens.world

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.foobuilders.world.entities.EntitySnapshot

final class EntityRenderer(cellSize: Float) {
  def render(shapes: ShapeRenderer, entities: Iterable[EntitySnapshot]): Unit = {
    shapes.begin(ShapeRenderer.ShapeType.Filled)

    entities.foreach { entity =>
      val visual  = entity.definition.visual
      val size    = cellSize * visual.scale
      val padding = (cellSize - size) * 0.5f

      shapes.setColor(visual.fillColor)
      shapes.rect(
        entity.position.x * cellSize + padding,
        entity.position.y * cellSize + padding,
        size,
        size
      )
    }

    shapes.end()
  }
}

object EntityRenderer {
  def apply(cellSize: Float): EntityRenderer = new EntityRenderer(cellSize)
}
