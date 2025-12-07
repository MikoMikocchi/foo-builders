package dev.foobuilders.core.world

import dev.foobuilders.core.sim.Simulation
import dev.foobuilders.shared.math.Vec2
import dev.foobuilders.shared.protocol.EntitySeed

object WorldFactory {
  def sandbox(): Simulation = {
    Simulation(
      Seq(
        EntitySeed("builder-1", Vec2(8d, 8d)),
        EntitySeed("hauler-1", Vec2(12d, 10d)),
      ),
    )
  }
}
