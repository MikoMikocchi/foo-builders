package com.foobuilders.game.screens

import com.badlogic.gdx.{ScreenAdapter, Gdx, InputMultiplexer}
import com.badlogic.gdx.graphics.{GL20, Color}
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.foobuilders.game.FooBuildersGame
import com.foobuilders.game.rendering.{GameCamera, Grid3D, WorldRenderer}
import com.foobuilders.game.world.GameWorld
import com.foobuilders.game.input.UnitInputHandler

import com.badlogic.gdx.graphics.g2d.{BitmapFont, SpriteBatch}
import com.badlogic.gdx.math.collision.Ray
import com.badlogic.gdx.math.Vector3

import com.badlogic.gdx.graphics.OrthographicCamera

class GameScreen(game: FooBuildersGame) extends ScreenAdapter {

  private var gameCamera: GameCamera = _
  private var grid3D: Grid3D = _
  private var world: GameWorld = _
  private var worldRenderer: WorldRenderer = _
  private var spriteBatch: SpriteBatch = _
  private var shapeRenderer: ShapeRenderer = _
  private var font: BitmapFont = _
  private var uiCamera: OrthographicCamera = _
  private var unitInputHandler: UnitInputHandler = _
  private var debugText: String = ""

  override def show(): Unit = {
    gameCamera = new GameCamera(
      Gdx.graphics.getWidth.toFloat,
      Gdx.graphics.getHeight.toFloat
    )
    // We will use InputMultiplexer instead of direct attach
    // gameCamera.attachInput()

    world = new GameWorld(100, 20, 100)
    world.generatePlatform()

    // Spawn some units
    world.spawnUnit(new Vector3(55, 1, 55))
    world.spawnUnit(new Vector3(60, 1, 55))
    world.spawnUnit(new Vector3(55, 1, 60))

    // Grid centered at platform (50, 0, 50) covering slightly more than 50x50
    grid3D =
      new Grid3D(gridSize = 60, cellSize = 1f, centerX = 50f, centerZ = 50f)
    worldRenderer = new WorldRenderer(world)

    spriteBatch = new SpriteBatch()
    shapeRenderer = new ShapeRenderer()
    font = new BitmapFont()

    uiCamera = new OrthographicCamera()
    uiCamera.setToOrtho(
      false,
      Gdx.graphics.getWidth.toFloat,
      Gdx.graphics.getHeight.toFloat
    )

    unitInputHandler = new UnitInputHandler(world, gameCamera)

    val multiplexer = new InputMultiplexer()
    multiplexer.addProcessor(unitInputHandler) // UI/Selection first
    multiplexer.addProcessor(gameCamera) // Camera movement second
    Gdx.input.setInputProcessor(multiplexer)
  }

  override def render(delta: Float): Unit = {
    update(delta)

    Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT)

    worldRenderer.render(gameCamera.camera)
    grid3D.render(gameCamera.camera)

    renderSelectionBox()
    renderDebug()
  }

  private def update(delta: Float): Unit = {
    gameCamera.update(delta)
    world.update(delta)
    performRaycast()
  }

  private def renderSelectionBox(): Unit = {
    unitInputHandler.getSelectionRect() match {
      case Some((x, y, w, h)) =>
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        shapeRenderer.setProjectionMatrix(uiCamera.combined)
        shapeRenderer.begin(ShapeType.Line)
        shapeRenderer.setColor(0f, 1f, 0f, 0.5f)

        // Input coords are Top-Left origin.
        // uiCamera is Bottom-Left origin.
        // y in input is distance from top.
        // y in uiCamera should be: height - y - h
        // But height - y is the top edge. so we draw down by h?
        // shapeRenderer.rect draws from bottom-left corner usually.
        // rect(x, y, w, h).
        // if we want top-left at (x, input_y):
        // bottom-left y = height - (input_y + h)

        val screenH = Gdx.graphics.getHeight.toFloat
        shapeRenderer.rect(x, screenH - y - h, w, h)

        shapeRenderer.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)
      case None =>
    }
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
    if (shapeRenderer != null) shapeRenderer.dispose()
    if (font != null) font.dispose()
  }
}
