package com.foobuilders

import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3

final class GameScreen extends ScreenAdapter {
  private val cellSize      = 1.0f
  private val gridHalfCells = 50

  private val pixelsPerUnit = 32.0f

  private val edgeScrollMarginPx = 24
  private val edgeScrollSpeed    = 18.0f // world units / second

  private val zoomStep = 0.10f
  private val minZoom  = 0.35f
  private val maxZoom  = 3.50f

  private val camera = new OrthographicCamera()
  private val shapes = new ShapeRenderer()

  private val target   = new Vector3(0.0f, 0.0f, 0.0f)
  private val tmpMove  = new Vector3()
  private val tmpWorld = new Vector3()

  private val input = new InputAdapter {
    override def scrolled(amountX: Float, amountY: Float): Boolean = {
      val factor   = 1.0f + (amountY * zoomStep)
      val nextZoom = (camera.zoom * factor).max(minZoom).min(maxZoom)
      camera.zoom = nextZoom

      true
    }
  }

  override def show(): Unit = {
    Gdx.input.setInputProcessor(input)

    // Default camera setup (persistent; do not reset every frame)
    resize(Gdx.graphics.getWidth, Gdx.graphics.getHeight)
    camera.position.set(0.0f, 0.0f, 0.0f)
    camera.zoom = 1.0f
    camera.update()
  }

  override def render(delta: Float): Unit = {
    Gdx.gl.glClearColor(0.08f, 0.09f, 0.12f, 1.0f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    updateEdgeScroll(delta)

    camera.position.set(target.x, target.y, 0.0f)
    camera.update()

    shapes.setProjectionMatrix(camera.combined)

    val min = -gridHalfCells * cellSize
    val max = gridHalfCells * cellSize

    // 2D grid on XY plane
    shapes.begin(ShapeRenderer.ShapeType.Line)
    shapes.setColor(new Color(0.18f, 0.20f, 0.26f, 1.0f))

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

    // Axis helpers
    shapes.setColor(new Color(0.85f, 0.25f, 0.25f, 1.0f)) // X axis
    shapes.line(min, 0.0f, max, 0.0f)
    shapes.setColor(new Color(0.25f, 0.85f, 0.25f, 1.0f)) // Y axis
    shapes.line(0.0f, min, 0.0f, max)
    shapes.end()

    // Hover highlight: unproject mouse coords to world
    tmpWorld.set(Gdx.input.getX.toFloat, Gdx.input.getY.toFloat, 0.0f)
    camera.unproject(tmpWorld)
    val cellX = Math.floor(tmpWorld.x / cellSize.toDouble).toInt
    val cellY = Math.floor(tmpWorld.y / cellSize.toDouble).toInt

    if (cellX >= -gridHalfCells && cellX < gridHalfCells && cellY >= -gridHalfCells && cellY < gridHalfCells) {
      val x0 = cellX * cellSize
      val y0 = cellY * cellSize

      shapes.begin(ShapeRenderer.ShapeType.Line)
      shapes.setColor(new Color(0.95f, 0.85f, 0.20f, 1.0f))
      shapes.line(x0, y0, x0 + cellSize, y0)
      shapes.line(x0 + cellSize, y0, x0 + cellSize, y0 + cellSize)
      shapes.line(x0 + cellSize, y0 + cellSize, x0, y0 + cellSize)
      shapes.line(x0, y0 + cellSize, x0, y0)
      shapes.end()
    }
  }

  private def updateEdgeScroll(delta: Float): Unit = {
    val screenW = Gdx.graphics.getWidth
    val screenH = Gdx.graphics.getHeight
    val mouseX  = Gdx.input.getX
    val mouseY  = Gdx.input.getY

    var xDir = 0.0f
    if (mouseX <= edgeScrollMarginPx) xDir = -1.0f
    else if (mouseX >= screenW - edgeScrollMarginPx) xDir = 1.0f

    var yDir = 0.0f
    // screen Y grows downward; top edge means "up" in world
    if (mouseY <= edgeScrollMarginPx) yDir = 1.0f
    else if (mouseY >= screenH - edgeScrollMarginPx) yDir = -1.0f

    if (xDir == 0.0f && yDir == 0.0f) return

    tmpMove.set(xDir, yDir, 0.0f)
    if (tmpMove.len2() < 0.0001f) return
    tmpMove.nor().scl(edgeScrollSpeed * delta)

    target.add(tmpMove)
  }

  override def resize(width: Int, height: Int): Unit = {
    camera.viewportWidth = width.toFloat / pixelsPerUnit
    camera.viewportHeight = height.toFloat / pixelsPerUnit
    camera.update()
  }

  override def hide(): Unit = {
    if (Gdx.input.getInputProcessor eq input) {
      Gdx.input.setInputProcessor(null)
    }
  }

  override def dispose(): Unit = {
    shapes.dispose()
  }
}
