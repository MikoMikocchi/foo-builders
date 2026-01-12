package com.foobuilders.screens.world

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector3

final class WorldCameraController(
    edgeScrollMarginPx: Int,
    edgeScrollSpeed: Float,
    zoomStep: Float,
    minZoom: Float,
    maxZoom: Float,
    pixelsPerUnit: Float = 32.0f
) {
  val camera: OrthographicCamera = new OrthographicCamera()

  private val target       = new Vector3(0.0f, 0.0f, 0.0f)
  private val tmpMove      = new Vector3()

  private val inputProcessor = new InputAdapter {
    override def scrolled(amountX: Float, amountY: Float): Boolean = {
      // amountY: +1 down, -1 up (usually)
      val factor  = 1.0f + (amountY * zoomStep)
      val nextZoom = (camera.zoom * factor).max(minZoom).min(maxZoom)
      camera.zoom = nextZoom

      true
    }
  }

  def installInputProcessor(): Unit = {
    Gdx.input.setInputProcessor(inputProcessor)
  }

  def uninstallInputProcessor(): Unit = {
    if (Gdx.input.getInputProcessor eq inputProcessor) {
      Gdx.input.setInputProcessor(null)
    }
  }

  def setDefaultCameraPose(): Unit = {
    // Persistent; do not reset every frame
    resize(Gdx.graphics.getWidth, Gdx.graphics.getHeight)
    camera.position.set(0.0f, 0.0f, 0.0f)
    camera.zoom = 1.0f
    camera.update()
  }

  def lookAtTarget(): Unit = {
    // 2D: keep camera centered on target
    camera.position.set(target.x, target.y, 0.0f)
  }

  def update(delta: Float): Unit = {
    updateEdgeScroll(delta)
  }

  def resize(width: Int, height: Int): Unit = {
    camera.viewportWidth = width.toFloat / pixelsPerUnit
    camera.viewportHeight = height.toFloat / pixelsPerUnit
    camera.update()
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
}

object WorldCameraController {
  def apply(
      edgeScrollMarginPx: Int,
      edgeScrollSpeed: Float,
      zoomStep: Float,
      minZoom: Float,
      maxZoom: Float,
      pixelsPerUnit: Float = 32.0f
  ): WorldCameraController =
    new WorldCameraController(
      edgeScrollMarginPx = edgeScrollMarginPx,
      edgeScrollSpeed = edgeScrollSpeed,
      zoomStep = zoomStep,
      minZoom = minZoom,
      maxZoom = maxZoom,
      pixelsPerUnit = pixelsPerUnit
    )
}
