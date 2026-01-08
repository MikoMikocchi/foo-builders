package com.foobuilders.game.screens

import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.foobuilders.game.FooBuildersGame
import com.foobuilders.game.rendering.{GameCamera, Grid3D, WorldRenderer}
import com.foobuilders.game.world.GameWorld

import com.badlogic.gdx.graphics.g2d.{BitmapFont, SpriteBatch}
import com.badlogic.gdx.math.collision.Ray

import com.badlogic.gdx.graphics.OrthographicCamera

class GameScreen(game: FooBuildersGame) extends ScreenAdapter {

  private var gameCamera: GameCamera = _
  private var grid3D: Grid3D = _
  private var world: GameWorld = _
  private var worldRenderer: WorldRenderer = _
  private var spriteBatch: SpriteBatch = _
  private var font: BitmapFont = _
  private var uiCamera: OrthographicCamera = _
  private var debugText: String = ""

  override def show(): Unit = {
    gameCamera = new GameCamera(
      Gdx.graphics.getWidth.toFloat,
      Gdx.graphics.getHeight.toFloat
    )
    gameCamera.attachInput()

    world = new GameWorld(100, 20, 100)
    world.generatePlatform()

    // Grid centered at platform (50, 0, 50) covering slightly more than 50x50
    grid3D =
      new Grid3D(gridSize = 60, cellSize = 1f, centerX = 50f, centerZ = 50f)
    worldRenderer = new WorldRenderer(world)

    spriteBatch = new SpriteBatch()
    font = new BitmapFont()

    uiCamera = new OrthographicCamera()
    uiCamera.setToOrtho(
      false,
      Gdx.graphics.getWidth.toFloat,
      Gdx.graphics.getHeight.toFloat
    )
  }

  override def render(delta: Float): Unit = {
    update(delta)

    Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT)

    worldRenderer.render(gameCamera.camera)
    grid3D.render(gameCamera.camera)

    renderDebug()
  }

  private def update(delta: Float): Unit = {
    gameCamera.update(delta)
    performRaycast()
  }

  private def performRaycast(): Unit = {
    val ray: Ray = gameCamera.camera.getPickRay(
      Gdx.input.getX.toFloat,
      Gdx.input.getY.toFloat
    )
    val hit = world.raycast(ray, 100f)

    debugText = hit match {
      case Some((x, y, z, block)) => s"Block: ${block.name} at ($x, $y, $z)"
      case None                   => "Block: None"
    }
    // Add camera position to debug
    debugText += s"\nCam: ${gameCamera.camera.position}"
  }

  private def renderDebug(): Unit = {
    spriteBatch.setProjectionMatrix(uiCamera.combined)
    spriteBatch.begin()
    font.draw(spriteBatch, debugText, 10, uiCamera.viewportHeight - 10)
    spriteBatch.end()
  }

  override def resize(width: Int, height: Int): Unit = {
    if (gameCamera != null) {
      gameCamera.resize(width, height)
    }
    if (uiCamera != null) {
      uiCamera.setToOrtho(false, width.toFloat, height.toFloat)
      uiCamera.update()
    }
  }

  override def dispose(): Unit = {
    if (grid3D != null) grid3D.dispose()
    if (worldRenderer != null) worldRenderer.dispose()
    if (spriteBatch != null) spriteBatch.dispose()
    if (font != null) font.dispose()
  }
}
