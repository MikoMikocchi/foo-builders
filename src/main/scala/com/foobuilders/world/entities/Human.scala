package com.foobuilders.world.entities

import com.badlogic.gdx.graphics.Color

/** Simple non-player human entity with a random-walk brain. */
object Human {
  val definition: EntityDefinition =
    EntityDefinition(
      kind = "human",
      visual = EntityVisual(fillColor = new Color(0.85f, 0.75f, 0.25f, 1.0f))
    )

  def randomWalker(stepCooldownTicks: Int = 3): EntityBrain =
    new RandomWalkBrain(stepCooldownTicks = stepCooldownTicks)

  def commandable(stepCooldownTicks: Int = 3): ActionQueueBrain =
    new ActionQueueBrain(fallback = randomWalker(stepCooldownTicks))
}

final class RandomWalkBrain(stepCooldownTicks: Int) extends EntityBrain {
  private var cooldown: Int = 0

  override def decide(self: EntityPerception, context: EntityContext): EntityIntent = {
    if (cooldown > 0) {
      cooldown -= 1
      return EntityIntent.Idle
    }

    cooldown = stepCooldownTicks

    val directions = List((1, 0), (-1, 0), (0, 1), (0, -1), (0, 0))

    val maybeStep = context.random.shuffle(directions).collectFirst {
      case (dx, dy) =>
        val targetLevel = surfaceLevelAt(self.position.x + dx, self.position.y + dy, context)
        val nextPos     = GridPosition(self.position.x + dx, self.position.y + dy, targetLevel)
        if (context.canOccupyFor(self.id, nextPos)) Some(nextPos) else None
    }.flatten

    maybeStep match {
      case Some(nextPos) =>
        val dx = nextPos.x - self.position.x
        val dy = nextPos.y - self.position.y
        val dz = nextPos.level - self.position.level
        EntityIntent(MoveIntent.Step(dx, dy, dz))
      case None => EntityIntent.Idle
    }
  }

  private def surfaceLevelAt(cellX: Int, cellY: Int, context: EntityContext): Int = {
    val height = math.round(context.map.columnMetadataAt(cellX, cellY).height)
    height.max(0).min(context.map.depth - 1)
  }
}
