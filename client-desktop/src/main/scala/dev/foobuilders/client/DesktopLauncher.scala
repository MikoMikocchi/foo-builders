package dev.foobuilders.client

import com.badlogic.gdx.backends.lwjgl3.{Lwjgl3Application, Lwjgl3ApplicationConfiguration}
import dev.foobuilders.core.world.WorldFactory

object DesktopLauncher {
  def main(args: Array[String]): Unit = {
    val config = Lwjgl3ApplicationConfiguration()
    config.setTitle("Foo Builders")
    config.useVsync(true)
    config.setWindowedMode(1280, 720)
    config.setForegroundFPS(60)
    // HiDPI: input mapping handled via backbuffer dimensions

    val simulation = WorldFactory.sandbox()
    Lwjgl3Application(new DesktopSimulationApp(simulation), config)
  }
}
