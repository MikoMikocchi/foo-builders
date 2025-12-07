package dev.foobuilders.client

import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.{Gdx, InputAdapter}
import dev.foobuilders.client.DesktopSimulationApp
import dev.foobuilders.core.sim.Simulation
import dev.foobuilders.shared.math.Vec2
import dev.foobuilders.shared.protocol.{EntitySeed, GameCommand}

final class DesktopInput(simulation: Simulation) extends InputAdapter {
  private val controlledId = "builder-1"

  override def keyDown(keycode: Int): Boolean = {
    keycode match {
      case Keys.SPACE =>
        val id = s"drone-${System.currentTimeMillis().toHexString}"
        val position = Vec2(math.random() * 24, math.random() * 14)
        simulation.enqueue(GameCommand.Spawn(EntitySeed(id, position)))
        true
      case Keys.UP =>
        push(Vec2(0d, 1.5d))
        true
      case Keys.DOWN =>
        push(Vec2(0d, -1.5d))
        true
      case Keys.LEFT =>
        push(Vec2(-1.5d, 0d))
        true
      case Keys.RIGHT =>
        push(Vec2(1.5d, 0d))
        true
      case _ => false
    }
  }

  override def touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = {
    // Map logical screen coords directly into world space; on macOS the event coords match logical size.
    val worldScale = DesktopSimulationApp.WorldScale.toDouble
    val worldX = screenX.toDouble / worldScale
    val worldY = (Gdx.graphics.getHeight() - screenY.toDouble) / worldScale
    val id = s"probe-${System.nanoTime().toHexString}"
    simulation.enqueue(GameCommand.Spawn(EntitySeed(id, Vec2(worldX, worldY))))
    true
  }

  private def push(delta: Vec2): Unit = {
    simulation.enqueue(GameCommand.AddImpulse(controlledId, delta))
  }
}
