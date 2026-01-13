package com.foobuilders.world

import com.foobuilders.world.entities.EntitySystem
import com.foobuilders.world.tiles.MaterialRegistry

final class WorldState(val map: VoxelMap, val materials: MaterialRegistry) {
  private var elapsedSeconds: Double = 0.0

  private val entitySystem = EntitySystem(map = map, materials = materials)

  def entities: EntitySystem = entitySystem

  def step(tickSeconds: Float): Unit =
    elapsedSeconds += tickSeconds.toDouble

  def totalElapsedSeconds: Double = elapsedSeconds
}
