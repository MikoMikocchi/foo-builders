package dev.foobuilders.client

import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.{Gdx, InputAdapter}
import com.badlogic.gdx.math.Vector3
import dev.foobuilders.client.DesktopSimulationApp
import dev.foobuilders.core.sim.Simulation
import dev.foobuilders.shared.math.Vec3
import dev.foobuilders.shared.protocol.{EntitySeed, GameCommand}

final class DesktopInput(
    simulation: Simulation,
    cameraController: OrbitCameraController,
    getEntityPosition: String => Option[Vec3]
) extends InputAdapter {
  private val controlledId = "builder-1"
  private val characterMoveSpeed = 12.0
  private val cameraMoveSpeed = 30.0
  private var wPressed = false
  private var aPressed = false
  private var sPressed = false
  private var dPressed = false

  def update(deltaSeconds: Double): Unit = {
    val forward = cameraController.getForwardDirection
    val right = cameraController.getRightDirection

    cameraController.getMode match {
      case CameraMode.Free =>
        // WASD moves camera
        val delta = new Vector3(0, 0, 0)
        val speed = (cameraMoveSpeed * deltaSeconds).toFloat

        if (wPressed) delta.add(forward.x * speed, 0f, forward.z * speed)
        if (sPressed) delta.add(-forward.x * speed, 0f, -forward.z * speed)
        if (dPressed) delta.add(right.x * speed, 0f, right.z * speed)
        if (aPressed) delta.add(-right.x * speed, 0f, -right.z * speed)

        if (delta.len2() > 0) {
          cameraController.moveTarget(delta)
        }

      case CameraMode.Follow(_) =>
        // WASD moves character
        var moveDelta = Vec3.Zero

        if (wPressed) {
          moveDelta = moveDelta + Vec3(forward.x * characterMoveSpeed, forward.z * characterMoveSpeed, 0d)
        }
        if (sPressed) {
          moveDelta = moveDelta + Vec3(-forward.x * characterMoveSpeed, -forward.z * characterMoveSpeed, 0d)
        }
        if (dPressed) {
          moveDelta = moveDelta + Vec3(right.x * characterMoveSpeed, right.z * characterMoveSpeed, 0d)
        }
        if (aPressed) {
          moveDelta = moveDelta + Vec3(-right.x * characterMoveSpeed, -right.z * characterMoveSpeed, 0d)
        }

        if (moveDelta.magnitude() > 0) {
          push(moveDelta * deltaSeconds)
        }
    }
  }

  override def keyDown(keycode: Int): Boolean = {
    keycode match {
      case Keys.SPACE =>
        val id = s"drone-${System.currentTimeMillis().toHexString}"
        val position = Vec3(math.random() * 24, math.random() * 14, 1d)
        simulation.enqueue(GameCommand.Spawn(EntitySeed(id, position)))
        true
      case Keys.TAB =>
        cameraController.toggleMode(controlledId)
        true
      case Keys.F =>
        // Center on character and switch to Follow mode
        getEntityPosition(controlledId).foreach { pos =>
          cameraController.centerOn(pos)
        }
        cameraController.setMode(CameraMode.Follow(controlledId))
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
