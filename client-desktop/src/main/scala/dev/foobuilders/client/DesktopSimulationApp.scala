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
import com.badlogic.gdx.graphics.{Color, GL20, PerspectiveCamera}
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.{ApplicationAdapter, Gdx, InputMultiplexer}
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import dev.foobuilders.core.sim.Simulation
import dev.foobuilders.core.world.WorldBounds
import dev.foobuilders.shared.protocol.*

final class DesktopSimulationApp(simulation: Simulation)
    extends ApplicationAdapter {
  private val world = simulation.world
  private var modelBatch: ModelBatch = _
  private var environment: Environment = _
  private var camera: PerspectiveCamera = _
  private var cameraController: OrbitCameraController = _
  private var desktopInput: DesktopInput = _
  private var entityModel: Model = _
  private var chunkMesh: Option[ChunkMesher.Mesh] = None

  private var latestSnapshot: WorldSnapshot = simulation.snapshot()

  override def create(): Unit = {
    modelBatch = new ModelBatch()
    environment = buildEnvironment()
    chunkMesh = ChunkMesher.build(world, DesktopSimulationApp.CellSize)
    val focus = chunkMesh
      .map(mesh => focusPoint(mesh.bounds))
      .getOrElse(new Vector3(0f, 0f, 0f))
    camera = buildCamera(focus)
    cameraController = new OrbitCameraController(camera, focus)
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

  private def buildCamera(target: Vector3): PerspectiveCamera = {
    val camera = new PerspectiveCamera(
      DesktopSimulationApp.CameraFov,
      Gdx.graphics.getWidth.toFloat,
      Gdx.graphics.getHeight.toFloat
    )
    camera.near = 0.1f
    camera.far = 500f
    camera.position.set(
      target.x + 12f,
      DesktopSimulationApp.InitialCameraHeight,
      target.z + 24f
    )
    camera.lookAt(target)
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

    modelBatch.begin(camera)
    chunkMesh.foreach { mesh =>
      modelBatch.render(mesh.terrain, environment)
      modelBatch.render(mesh.grid)
    }
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

  private def focusPoint(bounds: WorldBounds): Vector3 = {
    val centerX = (bounds.minX + bounds.maxX + 1).toFloat * DesktopSimulationApp.CellSize / 2f
    val centerZ = (bounds.minY + bounds.maxY + 1).toFloat * DesktopSimulationApp.CellSize / 2f
    new Vector3(centerX, 0f, centerZ)
  }

  override def dispose(): Unit = {
    Option(modelBatch).foreach(_.dispose())
    Option(entityModel).foreach(_.dispose())
    chunkMesh.foreach { mesh =>
      Option(mesh.terrain.model).foreach(_.dispose())
      Option(mesh.grid.model).foreach(_.dispose())
    }
  }
}

object DesktopSimulationApp {
  val CellSize: Float = 1f
  val EntityHeight: Float = 0.8f
  val CameraFov: Float = 67f
  val InitialCameraHeight: Float = 38f
}
