package com.foobuilders.game.screens

import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.foobuilders.game.FooBuildersGame
import com.foobuilders.game.rendering.{GameCamera, Grid3D}

class GameScreen(game: FooBuildersGame) extends ScreenAdapter {

  private var gameCamera: GameCamera = _
  private var grid3D: Grid3D = _

  override def show(): Unit = {
    gameCamera = new GameCamera(
      Gdx.graphics.getWidth.toFloat,
      Gdx.graphics.getHeight.toFloat
    )
    gameCamera.attachInput()
    grid3D = new Grid3D(gridSize = 20, cellSize = 1f)
  }

  override def render(delta: Float): Unit = {
    update(delta)

    Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT)

    grid3D.render(gameCamera.camera)
  }

  private def update(delta: Float): Unit = {
    gameCamera.update(delta)
  }

  override def resize(width: Int, height: Int): Unit = {
    if (gameCamera != null) {
      gameCamera.resize(width, height)
    }
  }

  override def dispose(): Unit = {
    if (grid3D != null) {
      grid3D.dispose()
    }
  }
}
