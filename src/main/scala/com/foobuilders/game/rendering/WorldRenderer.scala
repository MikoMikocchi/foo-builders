package com.foobuilders.game.rendering

import com.badlogic.gdx.graphics.{Camera, Color, GL20}
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.graphics.g3d.{
  Environment,
  Model,
  ModelBatch,
  ModelInstance
}
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.Gdx
import com.foobuilders.game.world.GameWorld
import com.foobuilders.game.world.blocks.BlockType
import scala.collection.mutable
import scala.jdk.CollectionConverters._

class WorldRenderer(world: GameWorld) {

  private val modelBatch = new ModelBatch()
  private val shapeRenderer = new ShapeRenderer()
  private val environment = new Environment()
  private val models = mutable.Map[BlockType, Model]()
  private val instances = mutable.ArrayBuffer[ModelInstance]()
  private var unitModel: Model = _
  private val tmpVec = new Vector3()

  initEnvironment()
  initModels()
  buildInstances()

  private def initEnvironment(): Unit = {
    environment.set(
      new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f)
    )
    environment.add(
      new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f)
    )
  }

  private def initModels(): Unit = {
    val modelBuilder = new ModelBuilder()

    // Create a model for Stone
    // In a real extensible system, we might iterate over all BlockType objects
    // For now, we manually register known types or use the properties from the BlockType trait

    val types = Seq(BlockType.Stone)

    types.foreach { blockType =>
      val model = modelBuilder.createBox(
        1f,
        1f,
        1f,
        new com.badlogic.gdx.graphics.g3d.Material(
          ColorAttribute.createDiffuse(blockType.color)
        ),
        Usage.Position | Usage.Normal
      )
      models.put(blockType, model)
    }

    // Unit Model (Blue Cylinder)
    unitModel = modelBuilder.createCylinder(
      0.8f,
      1.8f,
      0.8f,
      16,
      new com.badlogic.gdx.graphics.g3d.Material(
        ColorAttribute.createDiffuse(Color.BLUE)
      ),
      Usage.Position | Usage.Normal
    )
  }

  // Rebuilds the visual representation (ModelInstances) from the World data
  def buildInstances(): Unit = {
    instances.clear()

    for (
      x <- 0 until world.width; y <- 0 until world.height;
      z <- 0 until world.depth
    ) {
      val block = world.getBlock(x, y, z)
      if (block != BlockType.Air && models.contains(block)) {
        val instance = new ModelInstance(models(block))
        // Position is center of block. If grid is 0..50, center is i + 0.5
        instance.transform.setToTranslation(x + 0.5f, y + 0.5f, z + 0.5f)
        instances += instance
      }
    }
  }

  def render(camera: Camera): Unit = {
    // 1. Render Blocks and Units
    modelBatch.begin(camera)
    modelBatch.render(instances.asJava, environment)

    // Render Units
    // Ideally we cache ModelInstances if they don't change often,
    // but for dynamic units we create/update them.
    for (unit <- world.units) {
      val instance = new ModelInstance(unitModel)
      // Unit pivot is bottom? Cylinder is centered.
      // Unit position is foot?
      // Cylinder height 1.8. Center at y + 0.9
      instance.transform.setToTranslation(
        unit.position.x,
        unit.position.y + 0.9f,
        unit.position.z
      )
      modelBatch.render(instance, environment)
    }
    modelBatch.end()

    // 2. Render Selection Circles (3D) and Path Lines
    renderSelectionOverlays(camera)

    // 3. Render HP Bars (2D Screen)
    renderHPBars(camera)
  }

  private def renderSelectionOverlays(camera: Camera): Unit = {
    Gdx.gl.glEnable(GL20.GL_BLEND)
    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    Gdx.gl.glEnable(GL20.GL_DEPTH_TEST) // Ensure depth test is ON

    shapeRenderer.setProjectionMatrix(camera.combined)
    shapeRenderer.begin(ShapeType.Line)

    for (unit <- world.units if unit.selected) {
      // 1. Draw Path Line if moving
      if (unit.isMoving) {
        shapeRenderer.setColor(Color.YELLOW)
        shapeRenderer.line(unit.position, unit.targetPosition)

        // Optional: Draw target cross/point
        val t = unit.targetPosition
        val s = 0.2f
        shapeRenderer.line(t.x - s, t.y, t.z, t.x + s, t.y, t.z)
        shapeRenderer.line(t.x, t.y, t.z - s, t.x, t.y, t.z + s)
      }

      // 2. Draw Selection Circle
      shapeRenderer.setColor(0f, 1f, 0f, 0.8f)

      // Draw circle at feet
      // ShapeRenderer doesn't have 3D circle, we simulate with poly or ellipse on ground
      // XZ plane circle
      val segments = 16
      val radius = unit.radius * 1.5f
      val y = unit.position.y + 0.05f // slightly above ground

      for (i <- 0 until segments) {
        val angle1 = (i.toFloat / segments) * Math.PI.toFloat * 2f
        val angle2 = ((i + 1).toFloat / segments) * Math.PI.toFloat * 2f
        val x1 = unit.position.x + Math.cos(angle1).toFloat * radius
        val z1 = unit.position.z + Math.sin(angle1).toFloat * radius
        val x2 = unit.position.x + Math.cos(angle2).toFloat * radius
        val z2 = unit.position.z + Math.sin(angle2).toFloat * radius
        shapeRenderer.line(x1, y, z1, x2, y, z2)
      }
    }
    shapeRenderer.end()
    Gdx.gl.glDisable(GL20.GL_BLEND)
    Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)
  }

  private def renderHPBars(camera: Camera): Unit = {
    // Switch to Screen Space
    val width = Gdx.graphics.getWidth.toFloat
    val height = Gdx.graphics.getHeight.toFloat
    shapeRenderer.getProjectionMatrix.setToOrtho2D(0, 0, width, height)
    shapeRenderer.updateMatrices() // apply the matrix

    shapeRenderer.begin(ShapeType.Filled)

    val barWidth = 40f
    val barHeight = 5f

    for (unit <- world.units if unit.selected) {
      tmpVec.set(unit.position.x, unit.position.y + 2.2f, unit.position.z)
      camera.project(tmpVec) // Projects to screen coords (0,0 bottom left)

      val x = tmpVec.x - barWidth / 2
      val y = tmpVec.y

      // Background
      shapeRenderer.setColor(Color.RED)
      shapeRenderer.rect(x, y, barWidth, barHeight)

      // Health
      shapeRenderer.setColor(Color.GREEN)
      val hpRatio = unit.hp / unit.maxHp
      shapeRenderer.rect(x, y, barWidth * hpRatio, barHeight)
    }

    shapeRenderer.end()
  }

  def dispose(): Unit = {
    modelBatch.dispose()
    shapeRenderer.dispose()
    if (unitModel != null) unitModel.dispose()
    models.values.foreach(_.dispose())
  }
}
