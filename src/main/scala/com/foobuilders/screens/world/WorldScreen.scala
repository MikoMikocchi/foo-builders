package com.foobuilders.screens.world

import com.badlogic.gdx.{Gdx, Input, InputAdapter, InputMultiplexer}
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import com.foobuilders.world.entities.{EntityId, GridPosition, Human, MoveToAction}
import com.foobuilders.world.{VoxelMap, WorldGenerator, WorldSimulation, WorldState}
import com.foobuilders.world.tiles.{MaterialRegistry, Materials}

final class WorldScreen extends ScreenAdapter {
  private val cellSize     = 1.0f
  private val chunkSize    = 16
  private val chunkRadius  = 4
  private val levels       = 48
  private val surfaceLevel = 24

  /** Current fog config - can be swapped for day/night cycle */
  private var fogConfig: FogConfig = FogConfig.day

  private val voxelMap = VoxelMap(
    chunkSize = chunkSize,
    chunkRadius = chunkRadius,
    levels = levels,
    defaultMaterial = Materials.Air.id
  )
  private val gridHalfCells = voxelMap.halfCells

  private val materialRegistry = MaterialRegistry.default
  private val tileRenderer     = TileRenderer(cellSize = cellSize, map = voxelMap, materials = materialRegistry)
  private val entityRenderer   = EntityRenderer(cellSize = cellSize)
  private val worldState       = new WorldState(voxelMap, materialRegistry)
  private val simulation       = new WorldSimulation(worldState, ticksPerSecond = 6.0f)

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
  private val tmpWorld         = new Vector3()

  private var activeLevel: Int = 0
  private var lastSimTicks     = 0
  private var selectedEntity   = Option.empty[EntityId]

  private var inputMultiplexer: InputMultiplexer = _

  private val worldInput = new InputAdapter {
    override def touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = {
      screenToCell(screenX, screenY) match {
        case None => false
        case Some((cellX, cellY)) =>
          button match {
            case Input.Buttons.LEFT  =>
              selectEntityAt(cellX, cellY)
              true
            case Input.Buttons.RIGHT =>
              issueMoveOrder(cellX, cellY)
              true
            case _ => false
          }
      }
    }
  }

  override def show(): Unit = {
    inputMultiplexer = new InputMultiplexer(worldInput, cameraController.inputProcessor)
    Gdx.input.setInputProcessor(inputMultiplexer)
    cameraController.setDefaultCameraPose()

    // Default world: flat ground across all chunks with air above
    WorldGenerator.flatGround(voxelMap, Materials.Grass.id, surfaceLevel = surfaceLevel)
    activeLevel = surfaceLevel

    spawnTestHumans()
  }

  override def render(delta: Float): Unit = {
    // Clear to appropriate fog color based on current view level
    val clearColor =
      if (activeLevel > surfaceLevel) fogConfig.skyFogColor
      else fogConfig.depthFogColor
    Gdx.gl.glClearColor(clearColor.r, clearColor.g, clearColor.b, 1.0f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    updateLevelHotkeys()
    lastSimTicks = simulation.update(delta)

    cameraController.update(delta)
    cameraController.lookAtTarget()
    cameraController.camera.update()

    shapes.setProjectionMatrix(cameraController.camera.combined)

    tileRenderer.render(shapes, activeLevel = activeLevel, surfaceLevel = surfaceLevel, fogConfig = fogConfig)
    val entitySnapshots = worldState.entities.snapshots
    entityRenderer.render(shapes, entitySnapshots, selectedEntity)
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

    // Debug overlay: hovered tile + entity info
    val hoveredCell = hoverHighlighter.hoveredCell(cameraController.camera)

    val tileInfo = hoveredCell match {
      case None => s"Tile: (out of bounds) | Level: $activeLevel"
      case Some((cellX, cellY)) =>
        val matId = voxelMap.materialAt(cellX, cellY, activeLevel)
        val mat   = materialRegistry.resolve(matId)
        s"Tile: ($cellX,$cellY,$activeLevel) | Material: ${mat.displayName} (${matId.value})"
    }

    val hoveredEntity = hoveredCell.flatMap { case (cellX, cellY) =>
      entitySnapshots.find(e =>
        e.position.x == cellX && e.position.y == cellY && e.position.level == activeLevel
      )
    }

    val entityInfo = hoveredEntity match {
      case Some(entity) =>
        s"Entity: ${entity.definition.kind}#${entity.id.value} @ (${entity.position.x},${entity.position.y},${entity.position.level})"
      case None => "Entity: (none)"
    }

    val selectionInfo = selectedEntity.flatMap(id => entitySnapshots.find(_.id == id)) match {
      case Some(entity) =>
        s"Selected: ${entity.definition.kind}#${entity.id.value}"
      case None => "Selected: (none)"
    }

    val simInfo =
      s"Sim: ${simulation.totalTicks} ticks (+$lastSimTicks) | Active level: $activeLevel / ${levels - 1} | Surface: $surfaceLevel"

    uiBatch.setProjectionMatrix(uiCamera.combined)
    uiBatch.begin()
    uiFont.setColor(1.0f, 1.0f, 1.0f, 1.0f)
    uiFont.draw(uiBatch, tileInfo, 12.0f, uiCamera.viewportHeight - 12.0f)
    uiFont.draw(uiBatch, entityInfo, 12.0f, uiCamera.viewportHeight - 32.0f)
    uiFont.draw(uiBatch, selectionInfo, 12.0f, uiCamera.viewportHeight - 52.0f)
    uiFont.draw(uiBatch, simInfo, 12.0f, uiCamera.viewportHeight - 72.0f)
    uiBatch.end()
  }

  override def resize(width: Int, height: Int): Unit = {
    cameraController.resize(width, height)

    uiCamera.setToOrtho(false, width.toFloat, height.toFloat)
    uiCamera.update()
  }

  override def hide(): Unit = {
    if (Gdx.input.getInputProcessor eq inputMultiplexer) {
      Gdx.input.setInputProcessor(null)
    }
  }

  override def dispose(): Unit = {
    shapes.dispose()
    uiBatch.dispose()
    uiFont.dispose()
  }

  private def screenToCell(screenX: Int, screenY: Int): Option[(Int, Int)] = {
    tmpWorld.set(screenX.toFloat, screenY.toFloat, 0.0f)
    cameraController.camera.unproject(tmpWorld)

    val cellX = Math.floor(tmpWorld.x / cellSize.toDouble).toInt
    val cellY = Math.floor(tmpWorld.y / cellSize.toDouble).toInt

    if (!voxelMap.inBounds(cellX, cellY, activeLevel)) None else Some((cellX, cellY))
  }

  private def selectEntityAt(cellX: Int, cellY: Int): Unit = {
    val nowSeconds = worldState.totalElapsedSeconds
    val previous   = selectedEntity
    val maybeEntity = worldState.entities.snapshots.find(e =>
      e.position.x == cellX && e.position.y == cellY && e.position.level == activeLevel
    )

    // Deselect previous if selecting a different entity or empty space
    previous.filter(id => maybeEntity.forall(_.id != id)).foreach { id =>
      worldState.entities.setSelected(id, selectedFlag = false, nowSeconds = nowSeconds)
    }

    selectedEntity = maybeEntity.map(_.id)

    selectedEntity.foreach { id =>
      worldState.entities.setSelected(id, selectedFlag = true, nowSeconds = nowSeconds)
    }
  }

  private def issueMoveOrder(cellX: Int, cellY: Int): Unit = {
    val target = GridPosition(cellX, cellY, activeLevel)
    if (!materialRegistry.resolve(voxelMap.materialAt(cellX, cellY, activeLevel)).isWalkable) return

    val append = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)

    selectedEntity.foreach { id =>
      worldState.entities.command(id, MoveToAction(target), append = append)
    }
  }

  private def updateLevelHotkeys(): Unit = {
    import com.badlogic.gdx.Input

    if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_UP) || Gdx.input.isKeyJustPressed(Input.Keys.E)) {
      activeLevel = (activeLevel + 1).min(levels - 1)
    }
    if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
      activeLevel = (activeLevel - 1).max(0)
    }
  }

  private def spawnTestHumans(): Unit = {
    val startPosition = GridPosition(0, 0, surfaceLevel)
    worldState.entities.spawn(Human.definition, startPosition, Human.commandable())
  }
}
