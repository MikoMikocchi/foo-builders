package com.foobuilders.world

import com.foobuilders.world.tiles.MaterialId

final class TileMap(val gridHalfCells: Int, defaultMaterial: MaterialId) {
  val size: Int = gridHalfCells * 2

  private val materials: Array[MaterialId] = Array.fill(size * size)(defaultMaterial)

  def inBounds(cellX: Int, cellY: Int): Boolean =
    cellX >= -gridHalfCells && cellX < gridHalfCells && cellY >= -gridHalfCells && cellY < gridHalfCells

  def fill(material: MaterialId): Unit =
    var i = 0
    while (i < materials.length) {
      materials(i) = material
      i += 1
    }

  def setMaterial(cellX: Int, cellY: Int, material: MaterialId): Unit = {
    if (!inBounds(cellX, cellY)) return
    materials(index(cellX, cellY)) = material
  }

  def materialAt(cellX: Int, cellY: Int): MaterialId = {
    if (!inBounds(cellX, cellY)) return defaultMaterial
    materials(index(cellX, cellY))
  }

  def foreachCell(f: (Int, Int, MaterialId) => Unit): Unit = {
    var iy = 0
    while (iy < size) {
      val cellY = iy - gridHalfCells

      var ix = 0
      while (ix < size) {
        val cellX = ix - gridHalfCells
        val mat   = materials(iy * size + ix)
        f(cellX, cellY, mat)

        ix += 1
      }

      iy += 1
    }
  }

  private def index(cellX: Int, cellY: Int): Int = {
    val ix = cellX + gridHalfCells
    val iy = cellY + gridHalfCells
    iy * size + ix
  }
}

object TileMap {
  def apply(gridHalfCells: Int, defaultMaterial: MaterialId): TileMap =
    new TileMap(gridHalfCells = gridHalfCells, defaultMaterial = defaultMaterial)
}
