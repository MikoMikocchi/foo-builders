package dev.foobuilders.client

import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.{
  Environment,
  Material,
  Model,
  ModelBatch,
  ModelInstance
}
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.{Color, GL20, PerspectiveCamera}
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.{ApplicationAdapter, Gdx, InputMultiplexer}
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import dev.foobuilders.core.sim.Simulation
import dev.foobuilders.shared.protocol.*

final class DesktopSimulationApp(simulation: Simulation)
    extends ApplicationAdapter {
  private var shapeRenderer: ShapeRenderer = _
  private var modelBatch: ModelBatch = _
  private var environment: Environment = _
  private var camera: PerspectiveCamera = _
  private var cameraController: OrbitCameraController = _
  private var desktopInput: DesktopInput = _
  private var entityModel: Model = _

  private var latestSnapshot: WorldSnapshot = simulation.snapshot()

  override def create(): Unit = {
    shapeRenderer = new ShapeRenderer()
    modelBatch = new ModelBatch()
    environment = buildEnvironment()
    camera = buildCamera()
    cameraController =
      new OrbitCameraController(camera, DesktopSimulationApp.GridCenter)
    entityModel = buildEntityModel()
    desktopInput = new DesktopInput(simulation, cameraController)

    val inputMultiplexer = new InputMultiplexer()
    inputMultiplexer.addProcessor(cameraController)
    inputMultiplexer.addProcessor(desktopInput)
    Gdx.input.setInputProcessor(inputMultiplexer)
  }

  override def render(): Unit = {
    val deltaSeconds = Gdx.graphics.getDeltaTime().toDouble

    desktopInput.update(deltaSeconds)

    val events = simulation.step(deltaSeconds)
    events.collectFirst { case GameEvent.WorldAdvanced(snapshot) =>
      latestSnapshot = snapshot
    }

    drawSnapshot()
  }

  private def buildEnvironment(): Environment = {
    val env = new Environment()
    env.set(
      new ColorAttribute(ColorAttribute.AmbientLight, 0.7f, 0.7f, 0.7f, 1f)
    )
    env.add(new DirectionalLight().set(1f, 1f, 1f, -0.6f, -1.0f, -0.8f))
    env
  }

  private def buildCamera(): PerspectiveCamera = {
    val camera = new PerspectiveCamera(
      DesktopSimulationApp.CameraFov,
      Gdx.graphics.getWidth.toFloat,
      Gdx.graphics.getHeight.toFloat
    )
    camera.near = 0.1f
    camera.far = 500f
    camera.position.set(
      DesktopSimulationApp.GridCenter.x + 8f,
      DesktopSimulationApp.InitialCameraHeight,
      DesktopSimulationApp.GridCenter.z + 16f
    )
    camera.lookAt(DesktopSimulationApp.GridCenter)
    camera.up.set(Vector3.Y)
    camera.update()
    camera
  }

  private def buildEntityModel(): Model = {
    val builder = new ModelBuilder()
    val size = DesktopSimulationApp.CellSize * 0.8f
    val height = DesktopSimulationApp.EntityHeight
    builder.createBox(
      size,
      height,
      size,
      new Material(ColorAttribute.createDiffuse(Color.SKY)),
      Usage.Position | Usage.Normal
    )
  }

  private def drawSnapshot(): Unit = {
    Gdx.gl.glClearColor(0.05f, 0.07f, 0.09f, 1f)
    Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT)

    drawGrid()
    drawEntities()
  }

  private def drawGrid(): Unit = {
    val extent = DesktopSimulationApp.GridExtent
    val cell = DesktopSimulationApp.CellSize
    shapeRenderer.setProjectionMatrix(camera.combined)

    shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
    shapeRenderer.setColor(0.25f, 0.45f, 0.7f, 1f)
    (0 to DesktopSimulationApp.GridSize).foreach { i =>
      val v = i * cell
      shapeRenderer.line(v, 0f, 0f, v, 0f, extent)
      shapeRenderer.line(0f, 0f, v, extent, 0f, v)
    }

    shapeRenderer.setColor(Color.RED)
    shapeRenderer.line(0f, 0f, 0f, extent, 0f, 0f) // X axis
    shapeRenderer.setColor(Color.YELLOW)
    shapeRenderer.line(0f, 0f, 0f, 0f, 0f, extent) // Z axis
    shapeRenderer.end()
  }

  private def drawEntities(): Unit = {
    modelBatch.begin(camera)
    latestSnapshot.entities.foreach { entity =>
      val instance = new ModelInstance(entityModel)
      instance.transform.setToTranslation(
        entity.position.x.toFloat * DesktopSimulationApp.CellSize,
        entity.position.z.toFloat * DesktopSimulationApp.CellSize + DesktopSimulationApp.EntityHeight / 2f,
        entity.position.y.toFloat * DesktopSimulationApp.CellSize
      )
      modelBatch.render(instance, environment)
    }
    modelBatch.end()
  }

  override def dispose(): Unit = {
    Option(shapeRenderer).foreach(_.dispose())
    Option(modelBatch).foreach(_.dispose())
    Option(entityModel).foreach(_.dispose())
  }
}

object DesktopSimulationApp {
  val GridSize: Int = 64
  val CellSize: Float = 1f
  val GridExtent: Float = GridSize * CellSize
  val GridCenter: Vector3 = new Vector3(GridExtent / 2f, 0f, GridExtent / 2f)
  val EntityHeight: Float = 0.8f
  val CameraFov: Float = 67f
  val InitialCameraHeight: Float = 32f
  val WorldScale: Float = 64f
}
