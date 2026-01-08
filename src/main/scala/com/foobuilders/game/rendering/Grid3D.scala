package com.foobuilders.game.rendering

import com.badlogic.gdx.graphics.{Color, GL20}
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.Gdx

class Grid3D(val gridSize: Int = 20, val cellSize: Float = 1f, val centerX: Float = 0f, val centerZ: Float = 0f) {

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
    val startX = centerX - extent
    val startZ = centerZ - extent
    val endX = centerX + extent
    val endZ = centerZ + extent

    for (i <- 0 to gridSize) {
      val offset = i * cellSize
      
      // Lines parallel to Z axis (changing X)
      val x = startX + offset
      if (Math.abs(x) < 0.001f) shapeRenderer.setColor(axisZColor) else shapeRenderer.setColor(gridColor)
      shapeRenderer.line(x, 0f, startZ, x, 0f, endZ)

      // Lines parallel to X axis (changing Z)
      val z = startZ + offset
      if (Math.abs(z) < 0.001f) shapeRenderer.setColor(axisXColor) else shapeRenderer.setColor(gridColor)
      shapeRenderer.line(startX, 0f, z, endX, 0f, z)
    }

    shapeRenderer.end()
    Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)
  }

  def dispose(): Unit = {
    shapeRenderer.dispose()
  }
}
