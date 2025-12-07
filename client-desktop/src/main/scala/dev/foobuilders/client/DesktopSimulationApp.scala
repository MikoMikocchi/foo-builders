package dev.foobuilders.client

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.{Color, GL20}
import com.badlogic.gdx.{ApplicationAdapter, Gdx}
import dev.foobuilders.core.sim.Simulation
import dev.foobuilders.shared.protocol.*

final class DesktopSimulationApp(simulation: Simulation) extends ApplicationAdapter {
  private var shapeRenderer: ShapeRenderer = _
  private var latestSnapshot: WorldSnapshot = simulation.snapshot()

  override def create(): Unit = {
    shapeRenderer = new ShapeRenderer()
    Gdx.input.setInputProcessor(new DesktopInput(simulation))
  }

  override def render(): Unit = {
    val deltaSeconds = Gdx.graphics.getDeltaTime().toDouble

    val events = simulation.step(deltaSeconds)
    events.collectFirst { case GameEvent.WorldAdvanced(snapshot) =>
      latestSnapshot = snapshot
    }

    drawSnapshot()
  }

  private def drawSnapshot(): Unit = {
    Gdx.gl.glClearColor(0.05f, 0.07f, 0.09f, 1f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
    shapeRenderer.setColor(Color.SKY)

    latestSnapshot.entities.foreach { entity =>
      val radius = math.max(4f, 8f + (entity.energy.toFloat * 0.04f))
      shapeRenderer.circle(
        entity.position.x.toFloat * DesktopSimulationApp.WorldScale,
        entity.position.y.toFloat * DesktopSimulationApp.WorldScale,
        radius,
      )
    }

    shapeRenderer.end()
  }

  override def dispose(): Unit = {
    Option(shapeRenderer).foreach(_.dispose())
  }
}

object DesktopSimulationApp {
  val WorldScale: Float = 16f
}
