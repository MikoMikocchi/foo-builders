package dev.foobuilders.client

import com.badlogic.gdx.graphics.{Color, GL20}
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.{Material, Model, ModelInstance}
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.math.Vector3
import dev.foobuilders.core.world.{Chunk, World, WorldBounds}

object ChunkMesher {
  final case class Mesh(
      terrain: ModelInstance,
      grid: ModelInstance,
      bounds: WorldBounds
  )

  def build(world: World, cellSize: Float): Option[Mesh] = {
    world.loadedChunkBounds.map { bounds =>
      val terrainModel = buildTerrain(world, bounds, cellSize)
      val gridModel = buildGrid(world, bounds, cellSize)
      Mesh(
        new ModelInstance(terrainModel),
        new ModelInstance(gridModel),
        bounds
      )
    }
  }

  private def buildTerrain(
      world: World,
      bounds: WorldBounds,
      cell: Float
  ): Model = {
    val builder = new ModelBuilder()
    builder.begin()
    val material = new Material(
      ColorAttribute.createDiffuse(0.36f, 0.52f, 0.36f, 1f)
    )
    val xStart = bounds.minX
    val xEnd = bounds.maxX
    val yStart = bounds.minY
    val yEnd = bounds.maxY
    val patchSize = 64 // split mesh to stay under MeshBuilder's vertex cap
    var partIndex = 0

    var patchX = xStart
    while (patchX < xEnd) {
      val patchXEnd = math.min(patchX + patchSize, xEnd)

      var patchY = yStart
      while (patchY < yEnd) {
        val patchYEnd = math.min(patchY + patchSize, yEnd)

        val part = builder.part(
          s"terrain-$partIndex",
          GL20.GL_TRIANGLES,
          Usage.Position | Usage.Normal,
          material
        )

        var x = patchX
        while (x < patchXEnd) {
          var y = patchY
          while (y < patchYEnd) {
            val h00 = surfaceHeight(world, x, y)
            val h10 = surfaceHeight(world, x + 1, y)
            val h01 = surfaceHeight(world, x, y + 1)
            val h11 = surfaceHeight(world, x + 1, y + 1)

            val v00 = new Vector3(x * cell, h00 * cell, y * cell)
            val v10 = new Vector3((x + 1) * cell, h10 * cell, y * cell)
            val v11 = new Vector3((x + 1) * cell, h11 * cell, (y + 1) * cell)
            val v01 = new Vector3(x * cell, h01 * cell, (y + 1) * cell)

            part.triangle(v00, v10, v11)
            part.triangle(v00, v11, v01)

            y += 1
          }
          x += 1
        }

        partIndex += 1
        patchY = patchYEnd
      }

      patchX = patchXEnd
    }

    builder.end()
  }

  private def buildGrid(
      world: World,
      bounds: WorldBounds,
      cell: Float
  ): Model = {
    val builder = new ModelBuilder()
    builder.begin()
    val lift = 0.12f // чуть выше, чтобы линии не мерцали на поверхности
    val xStart = bounds.minX
    val xEnd = bounds.maxX
    val yStart = bounds.minY
    val yEnd = bounds.maxY
    val patchSize = 64
    var partIndex = 0

    var patchX = xStart
    while (patchX <= xEnd) {
      val patchXEnd = math.min(patchX + patchSize - 1, xEnd)

      var patchY = yStart
      while (patchY <= yEnd) {
        val patchYEnd = math.min(patchY + patchSize - 1, yEnd)

        val part = builder.part(
          s"grid-$partIndex",
          GL20.GL_LINES,
          Usage.Position | Usage.ColorUnpacked,
          new Material(ColorAttribute.createDiffuse(Color.CYAN))
        )
        part.setColor(0.15f, 0.35f, 0.6f, 1f)

        // Vertical lines (along Y in world coordinates, Z in render).
        var x = patchX
        while (x <= patchXEnd) {
          var y = patchY
          val verticalYMax = math.min(patchYEnd, yEnd - 1)
          while (y <= verticalYMax) {
            val h0 = surfaceHeight(world, x, y) * cell + lift
            val h1 = surfaceHeight(world, x, y + 1) * cell + lift
            val p0 = new Vector3(x * cell, h0, y * cell)
            val p1 = new Vector3(x * cell, h1, (y + 1) * cell)
            part.line(p0, p1)
            y += 1
          }
          x += 1
        }

        // Horizontal lines (along X).
        var y = patchY
        while (y <= patchYEnd) {
          val horizontalXMax = math.min(patchXEnd, xEnd - 1)
          var x = patchX
          while (x <= horizontalXMax) {
            val h0 = surfaceHeight(world, x, y) * cell + lift
            val h1 = surfaceHeight(world, x + 1, y) * cell + lift
            val p0 = new Vector3(x * cell, h0, y * cell)
            val p1 = new Vector3((x + 1) * cell, h1, y * cell)
            part.line(p0, p1)
            x += 1
          }
          y += 1
        }

        partIndex += 1
        patchY = patchYEnd + 1
      }

      patchX = patchXEnd + 1
    }

    builder.end()
  }

  private def surfaceHeight(world: World, x: Int, y: Int): Float = {
    math.max(0, world.getBlockHeight(x, y)).toFloat
  }
}
