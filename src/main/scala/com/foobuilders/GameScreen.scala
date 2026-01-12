package com.foobuilders

import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Plane
import com.badlogic.gdx.math.Vector3

final class GameScreen extends ScreenAdapter {
  private val cellSize      = 1.0f
  private val gridHalfCells = 50

  private val edgeScrollMarginPx = 24
  private val edgeScrollSpeed    = 18.0f // world units / second

  private val zoomStep          = 0.10f
  private val minCameraDistance = 6.0f
  private val maxCameraDistance = 120.0f

  private val camera = new PerspectiveCamera(
    67.0f,
    Gdx.graphics.getWidth.toFloat,
    Gdx.graphics.getHeight.toFloat
  )
  private val shapes      = new ShapeRenderer()
  private val groundPlane = new Plane(new Vector3(0.0f, 1.0f, 0.0f), 0.0f)

  private val target       = new Vector3(0.0f, 0.0f, 0.0f)
  private val tmpForward   = new Vector3()
  private val tmpRight     = new Vector3()
  private val tmpMove      = new Vector3()
  private val tmpCamOffset = new Vector3()

  private val input = new InputAdapter {
    override def scrolled(amountX: Float, amountY: Float): Boolean = {
      // amountY: +1 down, -1 up (usually)
      val factor = 1.0f + (amountY * zoomStep)
      tmpCamOffset.set(camera.position).sub(target)

      val currentDist = tmpCamOffset.len()
      if (currentDist > 0.0001f) {
        val nextDist = Math.max(minCameraDistance, Math.min(maxCameraDistance, currentDist * factor)).toFloat
        tmpCamOffset.setLength(nextDist)
        camera.position.set(target).add(tmpCamOffset)
      }

      true
    }
  }

  override def show(): Unit = {
    Gdx.input.setInputProcessor(input)

    // Default camera setup (persistent; do not reset every frame)
    camera.position.set(0.0f, 20.0f, 20.0f)
    camera.near = 0.1f
    camera.far = 250.0f
  }

  override def render(delta: Float): Unit = {
    Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
    Gdx.gl.glDepthMask(true)
    Gdx.gl.glClearColor(0.08f, 0.09f, 0.12f, 1.0f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT)

    updateEdgeScroll(delta)

    camera.lookAt(target)
    camera.update()

    shapes.setProjectionMatrix(camera.combined)

    val min = -gridHalfCells * cellSize
    val max = gridHalfCells * cellSize

    // Grid on XZ plane (y=0)
    shapes.begin(ShapeRenderer.ShapeType.Line)
    shapes.setColor(new Color(0.18f, 0.20f, 0.26f, 1.0f))

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

    // Axis helpers
    shapes.setColor(new Color(0.85f, 0.25f, 0.25f, 1.0f)) // X axis
    shapes.line(min, 0.0f, 0.0f, max, 0.0f, 0.0f)
    shapes.setColor(new Color(0.25f, 0.85f, 0.25f, 1.0f)) // Z axis
    shapes.line(0.0f, 0.0f, min, 0.0f, 0.0f, max)
    shapes.end()

    // Hover highlight: intersect mouse ray with ground plane (y=0)
    val ray = camera.getPickRay(Gdx.input.getX.toFloat, Gdx.input.getY.toFloat)
    val hit = new Vector3()
    if (Intersector.intersectRayPlane(ray, groundPlane, hit)) {
      val cellX = Math.floor(hit.x / cellSize.toDouble).toInt
      val cellZ = Math.floor(hit.z / cellSize.toDouble).toInt

      if (cellX >= -gridHalfCells && cellX < gridHalfCells && cellZ >= -gridHalfCells && cellZ < gridHalfCells) {
        val x0 = cellX * cellSize
        val z0 = cellZ * cellSize
        val y0 = 0.0f

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(new Color(0.95f, 0.85f, 0.20f, 1.0f))
        shapes.line(x0, y0, z0, x0 + cellSize, y0, z0)
        shapes.line(x0 + cellSize, y0, z0, x0 + cellSize, y0, z0 + cellSize)
        shapes.line(x0 + cellSize, y0, z0 + cellSize, x0, y0, z0 + cellSize)
        shapes.line(x0, y0, z0 + cellSize, x0, y0, z0)
        shapes.end()
      }
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

    var zDir = 0.0f
    // screen Y grows downward; top edge means "forward"
    if (mouseY <= edgeScrollMarginPx) zDir = 1.0f
    else if (mouseY >= screenH - edgeScrollMarginPx) zDir = -1.0f

    if (xDir == 0.0f && zDir == 0.0f) return

    // Move along ground plane relative to camera view
    tmpForward.set(camera.direction.x, 0.0f, camera.direction.z)
    if (tmpForward.len2() < 0.0001f) {
      tmpForward.set(0.0f, 0.0f, -1.0f)
    } else {
      tmpForward.nor()
    }

    tmpRight.set(tmpForward).crs(Vector3.Y).nor()
    tmpMove.set(tmpRight).scl(xDir).add(tmpForward.scl(zDir)).nor().scl(edgeScrollSpeed * delta)

    camera.position.add(tmpMove)
    target.add(tmpMove)
  }

  override def resize(width: Int, height: Int): Unit = {
    camera.viewportWidth = width.toFloat
    camera.viewportHeight = height.toFloat
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
