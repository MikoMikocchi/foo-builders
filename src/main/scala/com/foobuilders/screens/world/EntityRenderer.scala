package com.foobuilders.screens.world

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.foobuilders.world.entities.{EntityId, EntitySnapshot}

final class EntityRenderer(cellSize: Float) {
  def render(shapes: ShapeRenderer, entities: Iterable[EntitySnapshot], selected: Option[EntityId]): Unit = {
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

    selected.flatMap(id => entities.find(_.id == id)).foreach { entity =>
      val padding = cellSize * 0.08f
      shapes.begin(ShapeRenderer.ShapeType.Line)
      shapes.setColor(new Color(0.95f, 0.85f, 0.20f, 1.0f))
      shapes.rect(
        entity.position.x * cellSize + padding,
        entity.position.y * cellSize + padding,
        cellSize - (padding * 2.0f),
        cellSize - (padding * 2.0f)
      )
      shapes.end()
    }
  }
}

object EntityRenderer {
  def apply(cellSize: Float): EntityRenderer = new EntityRenderer(cellSize)
}
