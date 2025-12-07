package dev.foobuilders.core.world

import dev.foobuilders.core.sim.Simulation
import dev.foobuilders.shared.math.Vec3
import dev.foobuilders.shared.protocol.EntitySeed

object WorldFactory {
  def sandbox(): Simulation = {
    // Create a world with a ground layer and some test obstacles
    val world = createTestWorld()

    Simulation(
      world,
      Seq(
        EntitySeed("builder-1", Vec3(32d, 32d, 0d)),
        EntitySeed("hauler-1", Vec3(36d, 34d, 0d))
      )
    )
  }

  private def createTestWorld(): World = {
    var world = World.empty

    // Create ground layer at z=0
    for (x <- 0 until 64; y <- 0 until 64) {
      world = world.setBlock(x, y, 0, BlockType.Solid)
    }

    // Create some test obstacles (walls) for collision testing
    // Horizontal wall
    for (x <- 20 until 30) {
      world = world.setBlock(x, 25, 1, BlockType.Solid)
    }

    // Vertical wall
    for (y <- 20 until 30) {
      world = world.setBlock(40, y, 1, BlockType.Solid)
    }

    // Single block obstacle
    world = world.setBlock(45, 45, 1, BlockType.Solid)

    // Two-block high obstacle (should block movement)
    world = world.setBlock(50, 50, 1, BlockType.Solid)
    world = world.setBlock(50, 50, 2, BlockType.Solid)

    world
  }
}
