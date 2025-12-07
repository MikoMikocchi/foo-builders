package dev.foobuilders.core.world

import dev.foobuilders.core.sim.Simulation
import dev.foobuilders.shared.math.Vec3
import dev.foobuilders.shared.protocol.EntitySeed

object WorldFactory {
  private val defaultSeed = 1337L

  def sandbox(seed: Long = defaultSeed): Simulation = {
    val generator = new TerrainGenerator(seed)
    val world = World.generated(generator)

    // Preload a handful of chunks around the origin to avoid pop-in during movement.
    world.preload(Chunk.SizeX * 2, Chunk.SizeY * 2, radiusChunks = 4)

    val spawnX = Chunk.SizeX * 2
    val spawnY = Chunk.SizeY * 2
    val spawnZ = math.max(0, world.getBlockHeight(spawnX, spawnY)) + 1

    // Spawn a pair of drones near the center.
    Simulation(
      world,
      Seq(
        EntitySeed("builder-1", Vec3(spawnX.toDouble, spawnY.toDouble, spawnZ.toDouble)),
        EntitySeed(
          "hauler-1",
          Vec3(spawnX.toDouble + 4d, spawnY.toDouble + 2d, spawnZ.toDouble)
        )
      )
    )
  }
}
