package com.foobuilders.game.rendering

import com.badlogic.gdx.graphics.{Camera, Color, GL20}
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
import com.foobuilders.game.world.GameWorld
import com.foobuilders.game.world.blocks.BlockType
import scala.collection.mutable
import scala.jdk.CollectionConverters._

class WorldRenderer(world: GameWorld) {

  private val modelBatch = new ModelBatch()
  private val environment = new Environment()
  private val models = mutable.Map[BlockType, Model]()
  private val instances = mutable.ArrayBuffer[ModelInstance]()

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
    modelBatch.begin(camera)
    modelBatch.render(instances.asJava, environment)
    modelBatch.end()
  }

  def dispose(): Unit = {
    modelBatch.dispose()
    models.values.foreach(_.dispose())
  }
}
