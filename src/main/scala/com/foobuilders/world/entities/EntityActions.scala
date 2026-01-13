package com.foobuilders.world.entities

import scala.collection.mutable

final case class ActionResult(intent: EntityIntent = EntityIntent.Idle, completed: Boolean = false)

trait EntityAction {
  def next(self: EntityPerception, context: EntityContext): ActionResult
}

trait CommandableBrain {
  def setAction(action: EntityAction): Unit
  def enqueueAction(action: EntityAction): Unit
  def clearActions(): Unit
}

final class ActionQueueBrain(fallback: EntityBrain = ActionQueueBrain.idleFallback)
    extends EntityBrain
    with CommandableBrain {
  private val queue = mutable.Queue.empty[EntityAction]
  private var randomEnabled: Boolean = true

  override def decide(self: EntityPerception, context: EntityContext): EntityIntent = {
    queue.headOption match {
      case Some(action) =>
        val result = action.next(self, context)
        if (result.completed) {
          queue.dequeue()
        }
        result.intent
      case None =>
        if (randomEnabled) fallback.decide(self, context) else EntityIntent.Idle
    }
  }

  def setRandomEnabled(enabled: Boolean): Unit =
    randomEnabled = enabled

  override def setAction(action: EntityAction): Unit = {
    queue.clear()
    queue.enqueue(action)
  }

  override def enqueueAction(action: EntityAction): Unit =
    queue.enqueue(action)

  override def clearActions(): Unit =
    queue.clear()
}

object ActionQueueBrain {
  private val idleFallback: EntityBrain = new EntityBrain {
    override def decide(self: EntityPerception, context: EntityContext): EntityIntent = EntityIntent.Idle
  }
}

final case class MoveToAction(target: GridPosition) extends EntityAction {
  override def next(self: EntityPerception, context: EntityContext): ActionResult = {
    if (self.position == target) return ActionResult(EntityIntent.Idle, completed = true)

    val dx = target.x - self.position.x
    val dy = target.y - self.position.y
    val dz = target.level - self.position.level

    val stepX = math.signum(dx).toInt
    val stepY = math.signum(dy).toInt
    val stepZ = math.signum(dz).toInt

    if (stepX == 0 && stepY == 0 && stepZ == 0) {
      return ActionResult(EntityIntent.Idle, completed = true)
    }

    val nextPos = self.position.translate(stepX, stepY, stepZ)

    if (context.canOccupyFor(self.id, nextPos)) {
      val intent = EntityIntent(MoveIntent.Step(stepX, stepY, stepZ))
      val reached = nextPos == target
      ActionResult(intent, completed = reached)
    } else {
      ActionResult(EntityIntent.Idle, completed = false)
    }
  }
}
