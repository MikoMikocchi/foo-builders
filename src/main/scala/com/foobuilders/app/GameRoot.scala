package com.foobuilders.app

import com.badlogic.gdx.Game
import com.foobuilders.screens.world.WorldScreen

final class GameRoot extends Game {
  override def create(): Unit = {
    setScreen(new WorldScreen())
  }
}
