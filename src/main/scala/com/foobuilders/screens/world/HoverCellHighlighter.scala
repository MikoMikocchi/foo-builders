package com.foobuilders.screens.world

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Plane
import com.badlogic.gdx.math.Vector3

final class HoverCellHighlighter(cellSize: Float, gridHalfCells: Int) {
  private val groundPlane = new Plane(new Vector3(0.0f, 1.0f, 0.0f), 0.0f)
  private val hit         = new Vector3()

  def render(camera: PerspectiveCamera, shapes: ShapeRenderer, color: Color): Unit = {
    // Intersect mouse ray with ground plane (y=0)
    val ray = camera.getPickRay(Gdx.input.getX.toFloat, Gdx.input.getY.toFloat)
    if (!Intersector.intersectRayPlane(ray, groundPlane, hit)) return

    val cellX = Math.floor(hit.x / cellSize.toDouble).toInt
    val cellZ = Math.floor(hit.z / cellSize.toDouble).toInt

    if (cellX < -gridHalfCells || cellX >= gridHalfCells || cellZ < -gridHalfCells || cellZ >= gridHalfCells) return

    val x0 = cellX * cellSize
    val z0 = cellZ * cellSize
    val y0 = 0.0f

    shapes.begin(ShapeRenderer.ShapeType.Line)
    shapes.setColor(color)
    shapes.line(x0, y0, z0, x0 + cellSize, y0, z0)
    shapes.line(x0 + cellSize, y0, z0, x0 + cellSize, y0, z0 + cellSize)
    shapes.line(x0 + cellSize, y0, z0 + cellSize, x0, y0, z0 + cellSize)
    shapes.line(x0, y0, z0 + cellSize, x0, y0, z0)
    shapes.end()
  }
}

object HoverCellHighlighter {
  def apply(cellSize: Float, gridHalfCells: Int): HoverCellHighlighter =
    new HoverCellHighlighter(cellSize = cellSize, gridHalfCells = gridHalfCells)
}
