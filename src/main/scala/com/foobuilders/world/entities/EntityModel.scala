package com.foobuilders.world.entities

import com.badlogic.gdx.graphics.Color
import com.foobuilders.world.VoxelMap
import com.foobuilders.world.tiles.MaterialRegistry

import scala.util.Random

final case class GridPosition(x: Int, y: Int, level: Int) {
  def translate(dx: Int, dy: Int, dz: Int = 0): GridPosition =
    GridPosition(x + dx, y + dy, level + dz)
}

final case class EntityId(value: Long) extends AnyVal

final case class EntityDefinition(kind: String, visual: EntityVisual)

final case class EntityVisual(
    fillColor: Color,
    scale: Float = 0.85f
)

final case class EntitySnapshot(id: EntityId, definition: EntityDefinition, position: GridPosition)

final case class EntityPerception(id: EntityId, position: GridPosition, definition: EntityDefinition)

sealed trait MoveIntent
object MoveIntent {
  case object Stay extends MoveIntent
  final case class Step(dx: Int, dy: Int, dz: Int = 0) extends MoveIntent
}

final case class EntityIntent(move: MoveIntent = MoveIntent.Stay)
object EntityIntent {
  val Idle: EntityIntent = EntityIntent()
}

final case class EntityContext(
    map: VoxelMap,
    materials: MaterialRegistry,
    random: Random,
    canOccupy: (GridPosition, Option[EntityId]) => Boolean
) {
  def isWalkable(pos: GridPosition): Boolean =
    map.inBounds(pos.x, pos.y, pos.level) && materials.resolve(map.materialAt(pos.x, pos.y, pos.level)).isWalkable

  def canOccupyFor(id: EntityId, pos: GridPosition): Boolean =
    canOccupy(pos, Some(id))
}

trait EntityBrain {
  def decide(self: EntityPerception, context: EntityContext): EntityIntent
}
