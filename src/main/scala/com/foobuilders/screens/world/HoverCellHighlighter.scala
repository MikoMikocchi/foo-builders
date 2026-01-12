package com.foobuilders.screens.world

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3

final class HoverCellHighlighter(cellSize: Float, gridHalfCells: Int) {
  private val tmpWorld = new Vector3()

  def hoveredCell(camera: OrthographicCamera): Option[(Int, Int)] = {
    // Convert mouse screen coords to world coords
    tmpWorld.set(Gdx.input.getX.toFloat, Gdx.input.getY.toFloat, 0.0f)
    camera.unproject(tmpWorld)

    val cellX = Math.floor(tmpWorld.x / cellSize.toDouble).toInt
    val cellY = Math.floor(tmpWorld.y / cellSize.toDouble).toInt

    if (cellX < -gridHalfCells || cellX >= gridHalfCells || cellY < -gridHalfCells || cellY >= gridHalfCells) None
    else Some((cellX, cellY))
  }

  def render(camera: OrthographicCamera, shapes: ShapeRenderer, color: Color): Unit = {
    val (cellX, cellY) = hoveredCell(camera) match {
      case None             => return
      case Some((x0, y0))   => (x0, y0)
    }

    val x0 = cellX * cellSize
    val y0 = cellY * cellSize

    shapes.begin(ShapeRenderer.ShapeType.Line)
    shapes.setColor(color)
    shapes.line(x0, y0, x0 + cellSize, y0)
    shapes.line(x0 + cellSize, y0, x0 + cellSize, y0 + cellSize)
    shapes.line(x0 + cellSize, y0 + cellSize, x0, y0 + cellSize)
    shapes.line(x0, y0 + cellSize, x0, y0)
    shapes.end()
  }
}

object HoverCellHighlighter {
  def apply(cellSize: Float, gridHalfCells: Int): HoverCellHighlighter =
    new HoverCellHighlighter(cellSize = cellSize, gridHalfCells = gridHalfCells)
}
