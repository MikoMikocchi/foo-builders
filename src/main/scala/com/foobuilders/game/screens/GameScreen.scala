package com.foobuilders.game.screens

import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.foobuilders.game.FooBuildersGame

class GameScreen(game: FooBuildersGame) extends ScreenAdapter {

  override def show(): Unit = {
    // Prepare resources for this screen
  }

  override def render(delta: Float): Unit = {
    // Separate update logic could go here
    // update(delta)

    // Clear screen
    Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    game.batch.begin()
    // Drawing goes here
    game.batch.end()
  }

  override def resize(width: Int, height: Int): Unit = {
    // Handle viewport updates
  }

  override def dispose(): Unit = {
    // Dispose resources specific to this screen
  }
}
