package com.foobuilders.app

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

object GameMain {
  def main(args: Array[String]): Unit = {
    val config = new Lwjgl3ApplicationConfiguration()
    config.setTitle("Foo Builders")
    config.setWindowedMode(1280, 720)
    config.useVsync(true)
    new Lwjgl3Application(new GameRoot(), config)
  }
}
