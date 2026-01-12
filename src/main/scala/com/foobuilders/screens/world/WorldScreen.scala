package com.foobuilders.screens.world

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.foobuilders.world.TileMap
import com.foobuilders.world.tiles.MaterialRegistry
import com.foobuilders.world.tiles.Materials

final class WorldScreen extends ScreenAdapter {
  private val cellSize      = 1.0f
  private val gridHalfCells = 50

  private val materialRegistry = MaterialRegistry.default
  private val tileMap          = TileMap(gridHalfCells = gridHalfCells, defaultMaterial = Materials.Grass.id)
  private val tileRenderer     = TileRenderer(cellSize = cellSize, tileMap = tileMap, materials = materialRegistry)

  private val cameraController = WorldCameraController(
    edgeScrollMarginPx = 24,
    edgeScrollSpeed = 18.0f,
    zoomStep = 0.10f,
    minZoom = 0.35f,
    maxZoom = 3.50f,
    pixelsPerUnit = 32.0f
  )

  private val shapes           = new ShapeRenderer()
  private val uiCamera         = new OrthographicCamera()
  private val uiBatch          = new SpriteBatch()
  private val uiFont           = new BitmapFont()
  private val gridRenderer     = GridRenderer(cellSize = cellSize, gridHalfCells = gridHalfCells)
  private val hoverHighlighter = HoverCellHighlighter(cellSize = cellSize, gridHalfCells = gridHalfCells)

  override def show(): Unit = {
    cameraController.installInputProcessor()
    cameraController.setDefaultCameraPose()

    // Default world: grass platform across the whole grid
    tileMap.fill(Materials.Grass.id)
  }

  override def render(delta: Float): Unit = {
    Gdx.gl.glClearColor(0.08f, 0.09f, 0.12f, 1.0f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    cameraController.update(delta)
    cameraController.lookAtTarget()
    cameraController.camera.update()

    shapes.setProjectionMatrix(cameraController.camera.combined)

    tileRenderer.render(shapes)
    gridRenderer.render(shapes, color = new Color(0.18f, 0.20f, 0.26f, 1.0f))
    gridRenderer.renderAxes(
      shapes,
      xAxisColor = new Color(0.85f, 0.25f, 0.25f, 1.0f),
      zAxisColor = new Color(0.25f, 0.85f, 0.25f, 1.0f)
    )
    hoverHighlighter.render(
      camera = cameraController.camera,
      shapes = shapes,
      color = new Color(0.95f, 0.85f, 0.20f, 1.0f)
    )

    // Debug overlay: hovered tile info
    val infoText = hoverHighlighter.hoveredCell(cameraController.camera) match {
      case None => "Tile: (out of bounds)"
      case Some((cellX, cellY)) =>
        val matId = tileMap.materialAt(cellX, cellY)
        val mat   = materialRegistry.resolve(matId)
        s"Tile: ($cellX,$cellY) | Material: ${mat.displayName} (${matId.value})"
    }

    uiBatch.setProjectionMatrix(uiCamera.combined)
    uiBatch.begin()
    uiFont.setColor(1.0f, 1.0f, 1.0f, 1.0f)
    uiFont.draw(uiBatch, infoText, 12.0f, uiCamera.viewportHeight - 12.0f)
    uiBatch.end()
  }

  override def resize(width: Int, height: Int): Unit = {
    cameraController.resize(width, height)

    uiCamera.setToOrtho(false, width.toFloat, height.toFloat)
    uiCamera.update()
  }

  override def hide(): Unit = {
    cameraController.uninstallInputProcessor()
  }

  override def dispose(): Unit = {
    shapes.dispose()
    uiBatch.dispose()
    uiFont.dispose()
  }
}
