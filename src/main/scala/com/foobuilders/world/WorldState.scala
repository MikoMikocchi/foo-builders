package com.foobuilders.world

final class WorldState(val map: VoxelMap) {
  private var elapsedSeconds: Double = 0.0

  def step(tickSeconds: Float): Unit =
    elapsedSeconds += tickSeconds.toDouble

  def totalElapsedSeconds: Double = elapsedSeconds
}
