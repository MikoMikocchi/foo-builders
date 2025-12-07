package dev.foobuilders.shared.protocol

import dev.foobuilders.shared.math.Vec3

enum GameCommand {
  case Tick(deltaSeconds: Double)
  case Spawn(seed: EntitySeed)
  case AddImpulse(id: String, impulse: Vec3)
}

enum GameEvent {
  case WorldAdvanced(snapshot: WorldSnapshot)
  case EntitySpawned(state: EntityState)
  case CommandRejected(targetId: String, reason: String)
}

final case class WorldSnapshot(tick: Long, entities: Vector[EntityState])

final case class EntitySeed(
    id: String,
    position: Vec3,
    velocity: Vec3 = Vec3.Zero,
    energy: Double = 100d
)

final case class EntityState(
    id: String,
    position: Vec3,
    velocity: Vec3,
    energy: Double
)
