package com.foobuilders.world

final class WorldSimulation(world: WorldState, ticksPerSecond: Float) {
  private val clock = new SimulationClock(ticksPerSecond)

  def update(deltaSeconds: Float): Int =
    clock.update(deltaSeconds) { tickDt =>
      world.step(tickDt)
      world.entities.update(tickDt)
    }

  def totalTicks: Long = clock.ticksSoFar

  def tickDuration: Float = clock.tickDuration
}
