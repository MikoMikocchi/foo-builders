package com.foobuilders.screens.world

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

final class WorldScreen extends ScreenAdapter {
  private val cellSize      = 1.0f
  private val gridHalfCells = 50

  private val cameraController = WorldCameraController(
    edgeScrollMarginPx = 24,
    edgeScrollSpeed = 18.0f,
    zoomStep = 0.10f,
    minZoom = 0.35f,
    maxZoom = 3.50f,
    pixelsPerUnit = 32.0f
  )

  private val shapes           = new ShapeRenderer()
  private val gridRenderer     = GridRenderer(cellSize = cellSize, gridHalfCells = gridHalfCells)
  private val hoverHighlighter = HoverCellHighlighter(cellSize = cellSize, gridHalfCells = gridHalfCells)

  override def show(): Unit = {
    cameraController.installInputProcessor()
    cameraController.setDefaultCameraPose()
  }

  override def render(delta: Float): Unit = {
    Gdx.gl.glClearColor(0.08f, 0.09f, 0.12f, 1.0f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    cameraController.update(delta)
    cameraController.lookAtTarget()
    cameraController.camera.update()

    shapes.setProjectionMatrix(cameraController.camera.combined)

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
  }

  override def resize(width: Int, height: Int): Unit = {
    cameraController.resize(width, height)
  }

  override def hide(): Unit = {
    cameraController.uninstallInputProcessor()
  }

  override def dispose(): Unit = {
    shapes.dispose()
  }
}
