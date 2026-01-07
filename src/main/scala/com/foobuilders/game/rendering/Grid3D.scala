package com.foobuilders.game.rendering

import com.badlogic.gdx.graphics.{Color, GL20}
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.Gdx

class Grid3D(val gridSize: Int = 20, val cellSize: Float = 1f) {

  private val shapeRenderer = new ShapeRenderer()
  private val gridColor = new Color(0.3f, 0.3f, 0.3f, 1f)
  private val axisXColor = new Color(1f, 0f, 0f, 1f)
  private val axisZColor = new Color(0f, 0f, 1f, 1f)

  def render(camera: Camera): Unit = {
    Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)

    shapeRenderer.setProjectionMatrix(camera.combined)
    shapeRenderer.begin(ShapeRenderer.ShapeType.Line)

    val halfSize = gridSize / 2
    val extent = halfSize * cellSize

    for (i <- -halfSize to halfSize) {
      val pos = i * cellSize

      if (i == 0) {
        shapeRenderer.setColor(axisZColor)
      } else {
        shapeRenderer.setColor(gridColor)
      }
      shapeRenderer.line(
        -extent,
        0f,
        pos,
        extent,
        0f,
        pos
      )

      if (i == 0) {
        shapeRenderer.setColor(axisXColor)
      } else {
        shapeRenderer.setColor(gridColor)
      }
      shapeRenderer.line(
        pos,
        0f,
        -extent,
        pos,
        0f,
        extent
      )
    }

    shapeRenderer.end()
    Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)
  }

  def dispose(): Unit = {
    shapeRenderer.dispose()
  }
}
