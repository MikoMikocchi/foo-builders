package com.foobuilders.game.rendering

import com.badlogic.gdx.graphics.{Camera, Color, GL20}
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.graphics.g3d.{
  Environment,
  Model,
  ModelBatch,
  ModelInstance,
  Material
}
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.{
  BoxShapeBuilder,
  ConeShapeBuilder,
  CylinderShapeBuilder,
  SphereShapeBuilder
}
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.Gdx
import com.foobuilders.game.world.GameWorld
import com.foobuilders.game.world.blocks.BlockType
import com.foobuilders.game.entities.{UnitKind, UnitKinds, UnitShape, UnitStyle}
import scala.collection.mutable
import scala.jdk.CollectionConverters._

class WorldRenderer(world: GameWorld) {

  private val modelBatch = new ModelBatch()
  private val shapeRenderer = new ShapeRenderer()
  private val environment = new Environment()
  private val models = mutable.Map[BlockType, Model]()
  private val instances = mutable.ArrayBuffer[ModelInstance]()
  private val unitModels = mutable.Map[UnitKind, Model]()
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
    val blockModelBuilder = new ModelBuilder()

    // Create a model for Stone
    val types = Seq(BlockType.Stone)

    types.foreach { blockType =>
      val model = blockModelBuilder.createBox(
        1f,
        1f,
        1f,
        new Material(
          ColorAttribute.createDiffuse(blockType.color)
        ),
        Usage.Position | Usage.Normal
      )
      models.put(blockType, model)
    }

    unitModels.clear()
    UnitKinds.all.foreach { kind =>
      unitModels.put(kind, createUnitModel(kind))
    }
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
      unitModels.get(unit.kind).foreach { model =>
        val instance = new ModelInstance(model)
        // Unit pivot is now at feet (y=0 in model space)
        instance.transform.setToTranslation(
          unit.position.x,
          unit.position.y,
          unit.position.z
        )
        modelBatch.render(instance, environment)
      }
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
      val style = unit.style

      // 1. Draw Path Line if moving
      if (unit.isMoving) {
        shapeRenderer.setColor(style.secondary)
        shapeRenderer.line(unit.position, unit.targetPosition)

        // Optional: Draw target cross/point
        val t = unit.targetPosition
        val s = 0.2f
        shapeRenderer.line(t.x - s, t.y, t.z, t.x + s, t.y, t.z)
        shapeRenderer.line(t.x, t.y, t.z - s, t.x, t.y, t.z + s)
      }

      // 2. Draw Selection Circle
      shapeRenderer.setColor(style.primary)

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
      shapeRenderer.setColor(unit.style.secondary)
      val hpRatio = unit.hp / unit.maxHp
      shapeRenderer.rect(x, y, barWidth * hpRatio, barHeight)
    }

    shapeRenderer.end()
  }

  def dispose(): Unit = {
    modelBatch.dispose()
    shapeRenderer.dispose()
    unitModels.values.foreach(_.dispose())
    models.values.foreach(_.dispose())
  }

  private def createUnitModel(kind: UnitKind): Model = {
    kind.style.shape match {
      case UnitShape.ConeInfantry => createConeInfantry(kind.style)
      case UnitShape.BoxWorker    => createBoxWorker(kind.style)
    }
  }

  private def createConeInfantry(style: UnitStyle): Model = {
    val modelBuilder = new ModelBuilder()
    modelBuilder.begin()

    val bodyNode = modelBuilder.node()
    bodyNode.id = "body"
    bodyNode.translation.set(0, 0.6f, 0)

    val bodyPart = modelBuilder.part(
      "body",
      GL20.GL_TRIANGLES,
      Usage.Position | Usage.Normal,
      new Material(ColorAttribute.createDiffuse(style.primary))
    )
    ConeShapeBuilder.build(bodyPart, 0.8f, 1.2f, 0.8f, 16)

    val headNode = modelBuilder.node()
    headNode.id = "head"
    headNode.translation.set(0, 1.55f, 0)

    val headPart = modelBuilder.part(
      "head",
      GL20.GL_TRIANGLES,
      Usage.Position | Usage.Normal,
      new Material(ColorAttribute.createDiffuse(style.secondary))
    )
    SphereShapeBuilder.build(headPart, 0.7f, 0.7f, 0.7f, 16, 16)

    modelBuilder.end()
  }

  private def createBoxWorker(style: UnitStyle): Model = {
    val modelBuilder = new ModelBuilder()
    modelBuilder.begin()

    val bodyNode = modelBuilder.node()
    bodyNode.id = "body"
    bodyNode.translation.set(0f, 0.5f, 0f)
    val bodyPart = modelBuilder.part(
      "body",
      GL20.GL_TRIANGLES,
      Usage.Position | Usage.Normal,
      new Material(ColorAttribute.createDiffuse(style.primary))
    )
    BoxShapeBuilder.build(bodyPart, 1.0f, 1.0f, 0.85f)

    val packNode = modelBuilder.node()
    packNode.id = "pack"
    packNode.translation.set(0f, 0.8f, -0.45f)
    val packPart = modelBuilder.part(
      "pack",
      GL20.GL_TRIANGLES,
      Usage.Position | Usage.Normal,
      new Material(ColorAttribute.createDiffuse(style.secondary))
    )
    BoxShapeBuilder.build(packPart, 0.4f, 0.6f, 0.25f)

    val headNode = modelBuilder.node()
    headNode.id = "head"
    headNode.translation.set(0f, 1.25f, 0f)
    val headPart = modelBuilder.part(
      "head",
      GL20.GL_TRIANGLES,
      Usage.Position | Usage.Normal,
      new Material(ColorAttribute.createDiffuse(style.secondary))
    )
    SphereShapeBuilder.build(headPart, 0.55f, 0.55f, 0.55f, 16, 16)

    val helmetNode = modelBuilder.node()
    helmetNode.id = "helmet"
    helmetNode.translation.set(0f, 1.55f, 0f)
    val helmetPart = modelBuilder.part(
      "helmet",
      GL20.GL_TRIANGLES,
      Usage.Position | Usage.Normal,
      new Material(ColorAttribute.createDiffuse(style.primary))
    )
    CylinderShapeBuilder.build(helmetPart, 0.9f, 0.25f, 0.9f, 16)

    modelBuilder.end()
  }
}
