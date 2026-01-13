package com.foobuilders.screens.world

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.foobuilders.world.entities.{EntityId, EntitySnapshot}

final class EntityRenderer(cellSize: Float) {
  def render(
      shapes: ShapeRenderer,
      entities: Iterable[EntitySnapshot],
      selected: Option[EntityId],
      nowSeconds: Double,
      activeLevel: Int,
      surfaceLevel: Int,
      fogConfig: FogConfig
  ): Unit = {
    val fogColor = if (activeLevel > surfaceLevel) fogConfig.skyFogColor else fogConfig.depthFogColor

    shapes.begin(ShapeRenderer.ShapeType.Filled)

    entities.foreach { entity =>
      val visual  = entity.definition.visual
      val size    = cellSize * visual.scale
      val padding = (cellSize - size) * 0.5f

      val depthOffset = (activeLevel - entity.position.level).max(0)
      val fogAlpha    = visibilityForDepth(depthOffset, fogConfig.fogDepth, fogConfig.minVisibility)

      val drawColor = new Color()
      drawColor.r = lerp(fogColor.r, visual.fillColor.r, fogAlpha)
      drawColor.g = lerp(fogColor.g, visual.fillColor.g, fogAlpha)
      drawColor.b = lerp(fogColor.b, visual.fillColor.b, fogAlpha)
      drawColor.a = 1.0f

      shapes.setColor(drawColor)
      shapes.rect(
        entity.position.x * cellSize + padding,
        entity.position.y * cellSize + padding,
        size,
        size
      )
    }

    shapes.end()

    selected.flatMap(id => entities.find(_.id == id)).foreach { entity =>
      val pulse    = ((Math.sin(nowSeconds * 3.0) + 1.0) * 0.5).toFloat // 0..1
      val outerPad = cellSize * 0.05f
      val innerPad = cellSize * 0.12f

      val depthOffset = (activeLevel - entity.position.level).max(0)
      val fogAlpha    = visibilityForDepth(depthOffset, fogConfig.fogDepth, fogConfig.minVisibility)

      val lineA = new Color(0.96f, 0.88f, 0.32f, (0.35f + 0.20f * pulse) * fogAlpha)
      val lineB = new Color(0.94f, 0.80f, 0.20f, (0.20f + 0.15f * pulse) * fogAlpha)

      shapes.begin(ShapeRenderer.ShapeType.Line)
      shapes.setColor(lineA)
      shapes.rect(
        entity.position.x * cellSize + outerPad,
        entity.position.y * cellSize + outerPad,
        cellSize - (outerPad * 2.0f),
        cellSize - (outerPad * 2.0f)
      )
      shapes.setColor(lineB)
      shapes.rect(
        entity.position.x * cellSize + innerPad,
        entity.position.y * cellSize + innerPad,
        cellSize - (innerPad * 2.0f),
        cellSize - (innerPad * 2.0f)
      )
      shapes.end()
    }
  }

  private def visibilityForDepth(depthOffset: Int, fogDepth: Int, minAlpha: Float): Float = {
    if (depthOffset <= 0) 1.0f
    else {
      val k     = 2.5f / fogDepth.max(1).toFloat
      val decay = math.exp(-k * depthOffset).toFloat
      minAlpha + (1.0f - minAlpha) * decay
    }
  }

  private def lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}

object EntityRenderer {
  def apply(cellSize: Float): EntityRenderer = new EntityRenderer(cellSize)
}
