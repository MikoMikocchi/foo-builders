package com.foobuilders

import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20

final class GameScreen extends ScreenAdapter {
  override def render(delta: Float): Unit = {
    Gdx.gl.glClearColor(0.08f, 0.09f, 0.12f, 1.0f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
  }
}
