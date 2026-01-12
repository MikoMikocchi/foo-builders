package com.foobuilders.screens.world

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

final class GridRenderer(cellSize: Float, gridHalfCells: Int) {
  private val min = -gridHalfCells * cellSize
  private val max = gridHalfCells * cellSize

  def render(shapes: ShapeRenderer, color: Color): Unit = {
    // Grid on XZ plane (y=0)
    shapes.begin(ShapeRenderer.ShapeType.Line)
    shapes.setColor(color)

    var i = -gridHalfCells
    while (i <= gridHalfCells) {
      val x = i * cellSize
      val z = i * cellSize

      // Lines parallel to Z
      shapes.line(x, 0.0f, min, x, 0.0f, max)
      // Lines parallel to X
      shapes.line(min, 0.0f, z, max, 0.0f, z)

      i += 1
    }

    shapes.end()
  }

  def renderAxes(shapes: ShapeRenderer, xAxisColor: Color, zAxisColor: Color): Unit = {
    shapes.begin(ShapeRenderer.ShapeType.Line)

    // X axis
    shapes.setColor(xAxisColor)
    shapes.line(min, 0.0f, 0.0f, max, 0.0f, 0.0f)

    // Z axis
    shapes.setColor(zAxisColor)
    shapes.line(0.0f, 0.0f, min, 0.0f, 0.0f, max)

    shapes.end()
  }
}

object GridRenderer {
  def apply(cellSize: Float, gridHalfCells: Int): GridRenderer =
    new GridRenderer(cellSize = cellSize, gridHalfCells = gridHalfCells)
}
