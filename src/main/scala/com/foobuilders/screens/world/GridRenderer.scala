package com.foobuilders.screens.world

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

final class GridRenderer(cellSize: Float, gridHalfCells: Int) {
  private val min = -gridHalfCells * cellSize
  private val max = gridHalfCells * cellSize

  def render(shapes: ShapeRenderer, color: Color): Unit = {
    // 2D grid on XY plane
    shapes.begin(ShapeRenderer.ShapeType.Line)
    shapes.setColor(color)

    var i = -gridHalfCells
    while (i <= gridHalfCells) {
      val x = i * cellSize
      val y = i * cellSize

      // Vertical
      shapes.line(x, min, x, max)
      // Horizontal
      shapes.line(min, y, max, y)

      i += 1
    }

    shapes.end()
  }

  def renderAxes(shapes: ShapeRenderer, xAxisColor: Color, zAxisColor: Color): Unit = {
    shapes.begin(ShapeRenderer.ShapeType.Line)

    // X axis
    shapes.setColor(xAxisColor)
    shapes.line(min, 0.0f, max, 0.0f)

    // Y axis
    shapes.setColor(zAxisColor)
    shapes.line(0.0f, min, 0.0f, max)

    shapes.end()
  }
}

object GridRenderer {
  def apply(cellSize: Float, gridHalfCells: Int): GridRenderer =
    new GridRenderer(cellSize = cellSize, gridHalfCells = gridHalfCells)
}
