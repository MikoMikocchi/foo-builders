package com.foobuilders.game

import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.foobuilders.game.screens.GameScreen

class FooBuildersGame extends Game {
  // Global resources, like SpriteBatch, can be managed here
  var batch: SpriteBatch = _

  override def create(): Unit = {
    batch = new SpriteBatch()
    // Switch to the first screen
    setScreen(new GameScreen(this))
  }

  override def dispose(): Unit = {
    batch.dispose()
    super.dispose()
  }
}
