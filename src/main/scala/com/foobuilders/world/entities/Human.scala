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
      case (dx, dy) if context.canOccupyFor(self.id, self.position.translate(dx, dy)) => (dx, dy)
    }

    maybeStep match {
      case Some((dx, dy)) => EntityIntent(MoveIntent.Step(dx, dy))
      case None           => EntityIntent.Idle
    }
  }
}
