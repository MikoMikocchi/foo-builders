package dev.foobuilders.core.sim

import dev.foobuilders.core.world.World
import dev.foobuilders.shared.math.Vec3
import dev.foobuilders.shared.protocol.*

import scala.collection.mutable

final class Simulation(
    val world: World,
    initialSeeds: Seq[EntitySeed] = Seq.empty
) {
  private var tick: Long = 0L
  private val queued = mutable.Queue.empty[GameCommand]
  private val entities: mutable.LinkedHashMap[String, EntityState] =
    mutable.LinkedHashMap.from(
      initialSeeds.map(seed => seed.id -> seedToState(seed))
    )

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
      events: mutable.Builder[GameEvent, Vector[GameEvent]]
  ): Unit = {
    command match {
      case GameCommand.Spawn(seed) =>
        if (entities.contains(seed.id)) {
          events += GameEvent.CommandRejected(
            seed.id,
            s"Entity '${seed.id}' already exists"
          )
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
    val gravity = -20.0 // Gravity force pulling down (negative Z)

    entities.keys.foreach { id =>
      val current = entities(id)

      // Apply gravity to vertical velocity
      val velocityWithGravity = Vec3(
        current.velocity.x,
        current.velocity.y,
        current.velocity.z + gravity * deltaSeconds
      )

      val dampenedVelocity = velocityWithGravity * damping
      val displacement = dampenedVelocity * deltaSeconds
      val targetPosition = current.position + displacement

      // Check collisions and handle movement
      val (finalPosition, finalVelocity) = handleCollision(
        current.position,
        targetPosition,
        dampenedVelocity
      )

      // Final check: ensure entity is always at correct ground level when not moving vertically
      val finalCheckedPosition = {
        val checkX = math.floor(finalPosition.x).toInt
        val checkY = math.floor(finalPosition.y).toInt
        val checkGroundZ = world.getBlockHeight(checkX, checkY)
        val checkGroundLevel =
          if (checkGroundZ >= 0) checkGroundZ.toDouble else 0.0

        // If very close to ground (especially when not moving vertically), snap to exact ground level
        val distanceToGround = finalPosition.z - checkGroundLevel
        if (
          distanceToGround <= 0.05 || (finalVelocity.z == 0 && distanceToGround <= 0.15)
        ) {
          Vec3(finalPosition.x, finalPosition.y, checkGroundLevel)
        } else {
          finalPosition
        }
      }

      val newEnergy = math.max(0d, current.energy - energyDecay * deltaSeconds)

      entities.update(
        id,
        current.copy(
          position = finalCheckedPosition,
          velocity = finalVelocity,
          energy = newEnergy
        )
      )
    }
  }

  private def handleCollision(
      currentPos: Vec3,
      targetPos: Vec3,
      velocity: Vec3
  ): (Vec3, Vec3) = {
    val targetX = targetPos.x
    val targetY = targetPos.y
    val targetZ = targetPos.z

    val blockX = math.floor(targetX).toInt
    val blockY = math.floor(targetY).toInt
    val targetBlockZ = math.floor(targetZ).toInt

    // Get the ground height at target position
    val groundZ = world.getBlockHeight(blockX, blockY)
    // We assume that the support plane coincides with the top of the block at z=0,
    // but visually the cube should lie on the grid (Y=0), so we use groundZ.
    val groundLevel = if (groundZ >= 0) groundZ.toDouble else 0.0

    // First, check vertical collision with ground
    val isFalling = velocity.z < 0
    val distanceToGround = targetZ - groundLevel

    // If we're at or very close to ground level, snap to ground
    // Use a tighter threshold to prevent levitation
    if (distanceToGround <= 0.05 || (isFalling && distanceToGround <= 0.2)) {
      val snappedPosition = Vec3(targetX, targetY, groundLevel)
      val newVelocity = Vec3(velocity.x, velocity.y, 0d)
      return (snappedPosition, newVelocity)
    }

    // Check if there's a solid block at target position (horizontal collision)
    if (world.isBlocked(blockX, blockY, targetBlockZ)) {
      // Blocked horizontally - try to step up one block
      val stepUpZ = targetBlockZ + 1
      if (!world.isBlocked(blockX, blockY, stepUpZ)) {
        // Can step up - move to z+1
        val steppedPosition = Vec3(targetX, targetY, stepUpZ.toDouble)
        // Keep horizontal velocity, reset vertical velocity
        val newVelocity = Vec3(velocity.x, velocity.y, 0d)
        (steppedPosition, newVelocity)
      } else {
        // Cannot step up - stop horizontal movement, keep vertical
        val stoppedPosition = Vec3(currentPos.x, currentPos.y, targetZ)
        val newVelocity = Vec3(0d, 0d, velocity.z)
        (stoppedPosition, newVelocity)
      }
    } else {
      // No horizontal collision - continue with vertical movement
      // If falling and close to ground, snap to ground
      if (isFalling && distanceToGround > 0 && distanceToGround <= 0.5) {
        val landedPosition = Vec3(targetX, targetY, groundLevel)
        val newVelocity = Vec3(velocity.x, velocity.y, 0d)
        (landedPosition, newVelocity)
      } else {
        // Continue falling or moving
        (Vec3(targetX, targetY, targetZ), velocity)
      }
    }
  }

  private def drainQueued(): Seq[GameCommand] = {
    queued.dequeueAll(_ => true)
  }

  private def seedToState(seed: EntitySeed): EntityState = {
    // Ensure entity starts at correct ground level
    val blockX = math.floor(seed.position.x).toInt
    val blockY = math.floor(seed.position.y).toInt
    val groundZ = world.getBlockHeight(blockX, blockY)
    val groundLevel = if (groundZ >= 0) groundZ.toDouble else 0.0

    // Snap to ground level if too close
    val correctedPosition = if (seed.position.z <= groundLevel + 0.1) {
      Vec3(seed.position.x, seed.position.y, groundLevel)
    } else {
      seed.position
    }

    EntityState(seed.id, correctedPosition, seed.velocity, seed.energy)
  }

  private val maxVelocity = 25d
}

object Simulation {
  def apply(
      world: World,
      initialSeeds: Seq[EntitySeed] = Seq.empty
  ): Simulation = {
    new Simulation(world, initialSeeds)
  }
}
