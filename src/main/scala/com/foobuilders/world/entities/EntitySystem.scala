package com.foobuilders.world.entities

import com.foobuilders.world.VoxelMap
import com.foobuilders.world.tiles.MaterialRegistry

import scala.collection.mutable
import scala.util.Random

private final case class EntityInstance(
    id: EntityId,
    definition: EntityDefinition,
    brain: EntityBrain,
    var position: GridPosition
)

final class EntitySystem(
    map: VoxelMap,
    materials: MaterialRegistry,
    seed: Long = System.currentTimeMillis()
) {
  private val entities         = mutable.LinkedHashMap.empty[EntityId, EntityInstance]
  private val rng              = new Random(seed)
  private val selected         = mutable.Set.empty[EntityId]
  private val lastDeselectedAt = mutable.Map.empty[EntityId, Double]

  private var nextId: Long = 1L

  def spawn(definition: EntityDefinition, position: GridPosition, brain: EntityBrain): EntityId = {
    val id = EntityId(nextId)
    nextId += 1

    if (!canOccupy(position)) return id

    entities.update(id, EntityInstance(id, definition, brain, position))
    id
  }

  def snapshots: Iterable[EntitySnapshot] =
    entities.valuesIterator.map(e => EntitySnapshot(e.id, e.definition, e.position)).toList

  def setSelected(entityId: EntityId, selectedFlag: Boolean, nowSeconds: Double): Unit = {
    if (!entities.contains(entityId)) return

    if (selectedFlag) {
      selected += entityId
      lastDeselectedAt.remove(entityId)
      setRandomEnabled(entityId, enabled = false)
    } else {
      selected -= entityId
      lastDeselectedAt.update(entityId, nowSeconds)
      setRandomEnabled(entityId, enabled = false)
    }
  }

  def command(entityId: EntityId, action: EntityAction, append: Boolean): Boolean =
    commandableBrain(entityId) match {
      case Some(brain) =>
        if (append) brain.enqueueAction(action)
        else brain.setAction(action)
        true
      case None => false
    }

  def update(tickSeconds: Float, nowSeconds: Double): Unit = {
    val occupied = mutable.Map.from(occupiedPositionsById)

    val context = EntityContext(
      map = map,
      materials = materials,
      random = rng,
      canOccupy = (pos, ignore) => canOccupy(pos, ignoreId = ignore, occupied)
    )

    entities.valuesIterator.foreach { entity =>
      val shouldEnableRandom =
        if (selected.contains(entity.id)) false
        else
          lastDeselectedAt.get(entity.id) match {
            case Some(stamp) => (nowSeconds - stamp) >= 20.0
            case None        => true
          }

      setRandomEnabled(entity.id, enabled = shouldEnableRandom)

      val intent = entity.brain.decide(
        EntityPerception(entity.id, entity.position, entity.definition),
        context
      )

      intent.move match {
        case MoveIntent.Stay => // no-op
        case MoveIntent.Step(dx, dy, dz) =>
          val target = entity.position.translate(dx, dy, dz)
          val canGo  = canOccupy(target, ignoreId = Some(entity.id), occupied)

          if (canGo) {
            occupied.update(entity.id, target)
            entity.position = target
          }
      }
    }
  }

  private def canOccupy(
      pos: GridPosition,
      ignoreId: Option[EntityId] = None,
      occupied: mutable.Map[EntityId, GridPosition] = mutable.Map.from(occupiedPositionsById)
  ): Boolean = {
    if (!map.inBounds(pos.x, pos.y, pos.level)) return false
    val walkable = materials.resolve(map.materialAt(pos.x, pos.y, pos.level)).isWalkable
    if (!walkable) return false

    occupied.forall { case (entityId, existingPos) =>
      ignoreId.contains(entityId) || existingPos != pos
    }
  }

  private def occupiedPositionsById: Map[EntityId, GridPosition] =
    entities.view.mapValues(_.position).toMap

  private def commandableBrain(entityId: EntityId): Option[CommandableBrain] =
    entities.get(entityId).flatMap { instance =>
      instance.brain match {
        case brain: CommandableBrain => Some(brain)
        case _                       => None
      }
    }

  private def setRandomEnabled(entityId: EntityId, enabled: Boolean): Unit = {
    entities.get(entityId).foreach { instance =>
      instance.brain match {
        case brain: ActionQueueBrain => brain.setRandomEnabled(enabled)
        case _                       => // ignore
      }
    }
  }
}

object EntitySystem {
  def apply(map: VoxelMap, materials: MaterialRegistry, seed: Long = System.currentTimeMillis()): EntitySystem =
    new EntitySystem(map = map, materials = materials, seed = seed)
}
