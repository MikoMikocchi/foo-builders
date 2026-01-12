package com.foobuilders.screens.world

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Vector3

final class WorldCameraController(
    edgeScrollMarginPx: Int,
    edgeScrollSpeed: Float,
    zoomStep: Float,
    minCameraDistance: Float,
    maxCameraDistance: Float
) {
  val camera: PerspectiveCamera = new PerspectiveCamera(
    67.0f,
    Gdx.graphics.getWidth.toFloat,
    Gdx.graphics.getHeight.toFloat
  )

  private val target       = new Vector3(0.0f, 0.0f, 0.0f)
  private val tmpForward   = new Vector3()
  private val tmpRight     = new Vector3()
  private val tmpMove      = new Vector3()
  private val tmpCamOffset = new Vector3()

  private val inputProcessor = new InputAdapter {
    override def scrolled(amountX: Float, amountY: Float): Boolean = {
      // amountY: +1 down, -1 up (usually)
      val factor = 1.0f + (amountY * zoomStep)
      tmpCamOffset.set(camera.position).sub(target)

      val currentDist = tmpCamOffset.len()
      if (currentDist > 0.0001f) {
        val nextDist = Math
          .max(minCameraDistance, Math.min(maxCameraDistance, currentDist * factor))
          .toFloat
        tmpCamOffset.setLength(nextDist)
        camera.position.set(target).add(tmpCamOffset)
      }

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
    camera.position.set(0.0f, 20.0f, 20.0f)
    camera.near = 0.1f
    camera.far = 250.0f
  }

  def lookAtTarget(): Unit = {
    camera.lookAt(target)
  }

  def update(delta: Float): Unit = {
    updateEdgeScroll(delta)
  }

  def resize(width: Int, height: Int): Unit = {
    camera.viewportWidth = width.toFloat
    camera.viewportHeight = height.toFloat
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
}

object WorldCameraController {
  def apply(
      edgeScrollMarginPx: Int,
      edgeScrollSpeed: Float,
      zoomStep: Float,
      minCameraDistance: Float,
      maxCameraDistance: Float
  ): WorldCameraController =
    new WorldCameraController(
      edgeScrollMarginPx = edgeScrollMarginPx,
      edgeScrollSpeed = edgeScrollSpeed,
      zoomStep = zoomStep,
      minCameraDistance = minCameraDistance,
      maxCameraDistance = maxCameraDistance
    )
}
