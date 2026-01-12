package com.foobuilders.world

final class SimulationClock(ticksPerSecond: Float) {
  private val tickDurationSeconds = 1.0f / ticksPerSecond
  private var accumulator         = 0.0f
  private var totalTicks: Long    = 0L

  def update(deltaSeconds: Float)(tick: Float => Unit): Int = {
    accumulator += deltaSeconds

    var processed = 0
    while (accumulator >= tickDurationSeconds) {
      tick(tickDurationSeconds)
      accumulator -= tickDurationSeconds
      processed += 1
      totalTicks += 1
    }

    processed
  }

  def pendingRatio: Float =
    (accumulator / tickDurationSeconds).min(1.0f)

  def tickDuration: Float = tickDurationSeconds

  def ticksSoFar: Long = totalTicks
}
