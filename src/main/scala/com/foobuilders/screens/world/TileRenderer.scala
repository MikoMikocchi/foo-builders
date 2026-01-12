package com.foobuilders.screens.world

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.foobuilders.world.VoxelMap
import com.foobuilders.world.tiles.MaterialRegistry

final class TileRenderer(
    cellSize: Float,
    map: VoxelMap,
    materials: MaterialRegistry
) {
  private val scratchColor = new Color()

  /** Render visible tiles with distance fog.
    * @param fogConfig
    *   current fog configuration (can change for day/night)
    * @param surfaceLevel
    *   the ground level - above uses sky fog, below uses depth fog
    */
  def render(shapes: ShapeRenderer, activeLevel: Int, surfaceLevel: Int, fogConfig: FogConfig): Unit = {
    val startLevel = 0
    val fogDepth   = fogConfig.fogDepth
    val minAlpha   = fogConfig.minVisibility

    // Choose fog color based on whether we're above or below surface
    val fogColor = if (activeLevel > surfaceLevel) fogConfig.skyFogColor else fogConfig.depthFogColor

    shapes.begin(ShapeRenderer.ShapeType.Filled)

    val occluded = Array.fill[Boolean](map.width * map.height)(false)

    // Mark columns that already have blocking tiles above the active level (ceiling/ground)
    var scanLevel = map.depth - 1
    while (scanLevel > activeLevel) {
      map.foreachCellOnLevel(scanLevel) { (cellX, cellY, materialId) =>
        val idx  = columnIndex(cellX, cellY)
        val defn = materials.resolve(materialId)
        if (defn.blocksSight) occluded(idx) = true
      }
      scanLevel -= 1
    }

    var lvl = activeLevel
    while (lvl >= startLevel) {
      val depthOffset = activeLevel - lvl
      val alpha       = visibilityForDepth(depthOffset, fogDepth, minAlpha)

      map.foreachCellOnLevel(lvl) { (cellX, cellY, materialId) =>
        val index = columnIndex(cellX, cellY)
        if (!occluded(index)) {
          val defn = materials.resolve(materialId)
          if (defn.color.a > 0.0f) {
            // Blend tile color towards fog color based on depth
            scratchColor.r = lerp(fogColor.r, defn.color.r, alpha)
            scratchColor.g = lerp(fogColor.g, defn.color.g, alpha)
            scratchColor.b = lerp(fogColor.b, defn.color.b, alpha)
            scratchColor.a = 1.0f
            shapes.setColor(scratchColor)
            shapes.rect(cellX * cellSize, cellY * cellSize, cellSize, cellSize)
          }
          if (defn.blocksSight) occluded(index) = true
        }
      }

      lvl -= 1
    }

    shapes.end()
  }

  /** Exponential fog: fades quickly at first, then slows down, never fully disappearing */
  private def visibilityForDepth(depthOffset: Int, fogDepth: Int, minAlpha: Float): Float = {
    if (depthOffset <= 0) 1.0f
    else {
      // Exponential decay: alpha = minAlpha + (1 - minAlpha) * e^(-k * offset)
      val k     = 2.5f / fogDepth.max(1).toFloat
      val decay = math.exp(-k * depthOffset).toFloat
      minAlpha + (1.0f - minAlpha) * decay
    }
  }

  private def columnIndex(cellX: Int, cellY: Int): Int = {
    val ix = cellX + map.halfCells
    val iy = cellY + map.halfCells
    iy * map.width + ix
  }

  private def lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}

object TileRenderer {
  def apply(cellSize: Float, map: VoxelMap, materials: MaterialRegistry): TileRenderer =
    new TileRenderer(cellSize = cellSize, map = map, materials = materials)
}
