package dev.foobuilders.client

import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.{Gdx, InputAdapter}
import dev.foobuilders.client.DesktopSimulationApp
import dev.foobuilders.core.sim.Simulation
import dev.foobuilders.shared.math.Vec2
import dev.foobuilders.shared.protocol.{EntitySeed, GameCommand}

final class DesktopInput(simulation: Simulation) extends InputAdapter {
  private val controlledId = "builder-1"
  private val moveSpeed = 8.0
  private var wPressed = false
  private var aPressed = false
  private var sPressed = false
  private var dPressed = false

  def update(deltaSeconds: Double): Unit = {
    val moveDelta = (if (dPressed) Vec2(moveSpeed, 0d) else Vec2.Zero) +
      (if (aPressed) Vec2(-moveSpeed, 0d) else Vec2.Zero) +
      (if (wPressed) Vec2(0d, -moveSpeed) else Vec2.Zero) +
      (if (sPressed) Vec2(0d, moveSpeed) else Vec2.Zero)

    if (moveDelta.magnitude() > 0) {
      push(moveDelta * deltaSeconds)
    }
  }

  override def keyDown(keycode: Int): Boolean = {
    keycode match {
      case Keys.SPACE =>
        val id = s"drone-${System.currentTimeMillis().toHexString}"
        val position = Vec2(math.random() * 24, math.random() * 14)
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
    // Отключаем спавн по клику: управление кликом используется камерой.
    false
  }

  private def push(delta: Vec2): Unit = {
    simulation.enqueue(GameCommand.AddImpulse(controlledId, delta))
  }
}
