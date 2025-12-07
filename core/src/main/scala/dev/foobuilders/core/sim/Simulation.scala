package dev.foobuilders.core.sim

import dev.foobuilders.shared.math.Vec2
import dev.foobuilders.shared.protocol.*

import scala.collection.mutable

final class Simulation(initialSeeds: Seq[EntitySeed] = Seq.empty) {
  private var tick: Long = 0L
  private val queued = mutable.Queue.empty[GameCommand]
  private val entities: mutable.LinkedHashMap[String, EntityState] =
    mutable.LinkedHashMap.from(initialSeeds.map(seed => seed.id -> seedToState(seed)))

  def enqueue(command: GameCommand): Unit = {
    queued.enqueue(command)
  }

  /** Advance the world by `deltaSeconds`, applying queued commands. */
  def step(deltaSeconds: Double): Vector[GameEvent] = {
    val events = Vector.newBuilder[GameEvent]
    drainQueued().foreach(applyCommand(_, events))

    integrate(deltaSeconds)
    tick += 1

    events += GameEvent.WorldAdvanced(snapshot())
    events.result()
  }

  def snapshot(): WorldSnapshot = {
    WorldSnapshot(tick, entities.values.toVector)
  }

  private def applyCommand(
      command: GameCommand,
      events: mutable.Builder[GameEvent, Vector[GameEvent]],
  ): Unit = {
    command match {
      case GameCommand.Spawn(seed) =>
        if (entities.contains(seed.id)) {
          events += GameEvent.CommandRejected(seed.id, s"Entity '${seed.id}' already exists")
        } else {
          val state = seedToState(seed)
          entities.update(seed.id, state)
          events += GameEvent.EntitySpawned(state)
        }
      case GameCommand.AddImpulse(id, impulse) =>
        entities.get(id) match {
          case Some(state) =>
            val newVelocity = (state.velocity + impulse).clamp(maxVelocity)
            entities.update(id, state.copy(velocity = newVelocity))
          case None =>
            events += GameEvent.CommandRejected(id, s"Missing entity '$id'")
        }
      case GameCommand.Tick(_) => ()
    }
  }

  private def integrate(deltaSeconds: Double): Unit = {
    val damping = 0.98
    val energyDecay = 0.1

    entities.keys.foreach { id =>
      val current = entities(id)
      val dampenedVelocity = current.velocity * damping
      val displacement = dampenedVelocity * deltaSeconds
      val newPosition = current.position + displacement
      val newEnergy = math.max(0d, current.energy - energyDecay * deltaSeconds)

      entities.update(
        id,
        current.copy(position = newPosition, velocity = dampenedVelocity, energy = newEnergy),
      )
    }
  }

  private def drainQueued(): Seq[GameCommand] = {
    queued.dequeueAll(_ => true)
  }

  private def seedToState(seed: EntitySeed): EntityState = {
    EntityState(seed.id, seed.position, seed.velocity, seed.energy)
  }

  private val maxVelocity = 25d
}

object Simulation {
  def apply(initialSeeds: Seq[EntitySeed] = Seq.empty): Simulation = {
    new Simulation(initialSeeds)
  }
}
