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
  private val entities = mutable.LinkedHashMap.empty[EntityId, EntityInstance]
  private val rng      = new Random(seed)

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

  def update(tickSeconds: Float): Unit = {
    val occupied = mutable.Map.from(occupiedPositionsById)

    val context = EntityContext(
      map = map,
      materials = materials,
      random = rng,
      canOccupy = (pos, ignore) => canOccupy(pos, ignoreId = ignore, occupied)
    )

    entities.valuesIterator.foreach { entity =>
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
}

object EntitySystem {
  def apply(map: VoxelMap, materials: MaterialRegistry, seed: Long = System.currentTimeMillis()): EntitySystem =
    new EntitySystem(map = map, materials = materials, seed = seed)
}
