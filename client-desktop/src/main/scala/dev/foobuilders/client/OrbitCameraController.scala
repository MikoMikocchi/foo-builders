package dev.foobuilders.client

import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3

final class OrbitCameraController(
    camera: PerspectiveCamera,
    target: Vector3,
    initialYaw: Float = 45f,
    initialPitch: Float = 45f,
    initialDistance: Float = 24f
) extends InputAdapter {
  private var yaw: Float = initialYaw
  private var pitch: Float = initialPitch
  private var distance: Float = initialDistance
  private var lastX: Int = 0
  private var lastY: Int = 0
  private var draggingButton: Int = -1

  updateCamera()

  override def scrolled(amountX: Float, amountY: Float): Boolean = {
    val newDistance = distance + amountY * 1.5f
    distance = MathUtils.clamp(newDistance, 6f, 120f)
    updateCamera()
    true
  }

  override def touchDown(
      screenX: Int,
      screenY: Int,
      pointer: Int,
      button: Int
  ): Boolean = {
    if (button == Buttons.RIGHT) {
      draggingButton = button
      lastX = screenX
      lastY = screenY
      true
    } else {
      false
    }
  }

  override def touchDragged(
      screenX: Int,
      screenY: Int,
      pointer: Int
  ): Boolean = {
    if (draggingButton == Buttons.RIGHT) {
      val dx = screenX - lastX
      val dy = screenY - lastY

      yaw -= dx * 0.4f
      pitch = MathUtils.clamp(pitch + dy * 0.3f, 15f, 80f)

      updateCamera()
      lastX = screenX
      lastY = screenY
      true
    } else {
      false
    }
  }

  override def touchUp(
      screenX: Int,
      screenY: Int,
      pointer: Int,
      button: Int
  ): Boolean = {
    draggingButton = -1
    false
  }

  def getYaw: Float = yaw

  def getForwardDirection: Vector3 = {
    val yawRad = MathUtils.degreesToRadians * yaw
    // Forward direction is from camera to target, projected onto horizontal plane
    // Since camera looks at target, forward is opposite of camera's position relative to target
    val forwardX = -math.sin(yawRad).toFloat
    val forwardZ = -math.cos(yawRad).toFloat
    new Vector3(forwardX, 0f, forwardZ).nor()
  }

  def getRightDirection: Vector3 = {
    val forward = getForwardDirection
    // Right is forward rotated 90 degrees clockwise (in horizontal plane)
    new Vector3(-forward.z, 0f, forward.x).nor()
  }

  private def updateCamera(): Unit = {
    val yawRad = MathUtils.degreesToRadians * yaw
    val pitchRad = MathUtils.degreesToRadians * pitch

    val horizontal = math.cos(pitchRad).toFloat * distance
    val camX = target.x + math.sin(yawRad).toFloat * horizontal
    val camY = target.y + math.sin(pitchRad).toFloat * distance
    val camZ = target.z + math.cos(yawRad).toFloat * horizontal

    camera.position.set(camX, camY, camZ)
    camera.lookAt(target)
    camera.up.set(Vector3.Y)
    camera.update()
  }
}
