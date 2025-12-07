package dev.foobuilders.client

import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.{Gdx, InputAdapter}
import dev.foobuilders.client.DesktopSimulationApp
import dev.foobuilders.core.sim.Simulation
import dev.foobuilders.shared.math.Vec3
import dev.foobuilders.shared.protocol.{EntitySeed, GameCommand}

final class DesktopInput(
    simulation: Simulation,
    cameraController: OrbitCameraController
) extends InputAdapter {
  private val controlledId = "builder-1"
  private val moveSpeed = 12.0
  private var wPressed = false
  private var aPressed = false
  private var sPressed = false
  private var dPressed = false

  def update(deltaSeconds: Double): Unit = {
    val forward = cameraController.getForwardDirection
    val right = cameraController.getRightDirection

    var moveDelta = Vec3.Zero

    if (wPressed) {
      // Forward relative to camera
      moveDelta =
        moveDelta + Vec3(forward.x * moveSpeed, forward.z * moveSpeed, 0d)
    }
    if (sPressed) {
      // Backward relative to camera
      moveDelta =
        moveDelta + Vec3(-forward.x * moveSpeed, -forward.z * moveSpeed, 0d)
    }
    if (dPressed) {
      // Right relative to camera
      moveDelta = moveDelta + Vec3(right.x * moveSpeed, right.z * moveSpeed, 0d)
    }
    if (aPressed) {
      // Left relative to camera
      moveDelta =
        moveDelta + Vec3(-right.x * moveSpeed, -right.z * moveSpeed, 0d)
    }

    if (moveDelta.magnitude() > 0) {
      push(moveDelta * deltaSeconds)
    }
  }

  override def keyDown(keycode: Int): Boolean = {
    keycode match {
      case Keys.SPACE =>
        val id = s"drone-${System.currentTimeMillis().toHexString}"
        val position = Vec3(math.random() * 24, math.random() * 14, 1d)
        simulation.enqueue(GameCommand.Spawn(EntitySeed(id, position)))
        true
      case Keys.W =>
        wPressed = true
        true
      case Keys.A =>
        aPressed = true
        true
      case Keys.S =>
        sPressed = true
        true
      case Keys.D =>
        dPressed = true
        true
      case _ => false
    }
  }

  override def keyUp(keycode: Int): Boolean = {
    keycode match {
      case Keys.W =>
        wPressed = false
        true
      case Keys.A =>
        aPressed = false
        true
      case Keys.S =>
        sPressed = false
        true
      case Keys.D =>
        dPressed = false
        true
      case _ => false
    }
  }

  override def touchDown(
      screenX: Int,
      screenY: Int,
      pointer: Int,
      button: Int
  ): Boolean = {
    // Disable spawn on click: click control is used by the camera.
    false
  }

  private def push(delta: Vec3): Unit = {
    simulation.enqueue(GameCommand.AddImpulse(controlledId, delta))
  }
}
