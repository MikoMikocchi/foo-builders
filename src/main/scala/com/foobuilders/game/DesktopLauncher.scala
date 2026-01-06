package com.foobuilders.game

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

object DesktopLauncher {
  def main(args: Array[String]): Unit = {
    val config = new Lwjgl3ApplicationConfiguration()
    config.setTitle("Foo Builders")
    config.setWindowedMode(1200, 800)
    config.setForegroundFPS(60)
    new Lwjgl3Application(new FooBuildersGame(), config)
  }
}
