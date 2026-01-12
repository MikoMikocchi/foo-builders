package com.foobuilders

import com.badlogic.gdx.Game

final class GameRoot extends Game {
  override def create(): Unit = {
    setScreen(new GameScreen())
  }
}
