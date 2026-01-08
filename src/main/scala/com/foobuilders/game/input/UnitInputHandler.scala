package com.foobuilders.game.input

import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.{Vector2, Vector3, Intersector, Plane}
import com.badlogic.gdx.math.collision.Ray
import com.foobuilders.game.world.GameWorld
import com.foobuilders.game.rendering.GameCamera
import com.foobuilders.game.entities.GameUnit
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class UnitInputHandler(world: GameWorld, camera: GameCamera) extends InputAdapter {

  private val dragStart = new Vector2()
  private val currentPos = new Vector2()
  private var isDragging = false
  private val dragThreshold = 10f // pixels
  private val groundPlane = new Plane(Vector3.Y, 0f)

  // Exposed for rendering the selection box
  def getSelectionRect(): Option[(Float, Float, Float, Float)] = {
    if (isDragging) {
      val minX = Math.min(dragStart.x, currentPos.x)
      val maxX = Math.max(dragStart.x, currentPos.x)
      val minY = Math.min(dragStart.y, currentPos.y)
      val maxY = Math.max(dragStart.y, currentPos.y)
      Some((minX, minY, maxX - minX, maxY - minY))
    } else {
      None
    }
  }

  override def touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = {
    if (button == Input.Buttons.LEFT) {
      dragStart.set(screenX.toFloat, screenY.toFloat)
      currentPos.set(screenX.toFloat, screenY.toFloat)
      isDragging = true
      // Don't consume yet, wait to see if it's a drag or click
      // But we must return true to receive touchUp/Dragged
      true
    } else if (button == Input.Buttons.RIGHT) {
      // Right click anywhere deselects
      deselectAll()
      // Return false so GameCamera can handle rotation if needed
      false
    } else {
      false
    }
  }

  override def touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean = {
    if (isDragging) {
      currentPos.set(screenX.toFloat, screenY.toFloat)
      true
    } else {
      false
    }
  }

  override def touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = {
    if (button != Input.Buttons.LEFT || !isDragging) {
      return false
    }

    isDragging = false
    currentPos.set(screenX.toFloat, screenY.toFloat)
    
    val dragDist = dragStart.dst(currentPos)
    
    if (dragDist > dragThreshold) {
      // Box Selection
      performBoxSelection()
    } else {
      // Click
      handleLeftClick(screenX, screenY)
    }
    true
  }

  private def deselectAll(): Unit = {
    world.units.foreach(_.setSelected(false))
  }

  private def performBoxSelection(): Unit = {
    // Normalizing coordinates for comparison
    val minX = Math.min(dragStart.x, currentPos.x)
    val maxX = Math.max(dragStart.x, currentPos.x)
    val minY = Math.min(dragStart.y, currentPos.y)
    val maxY = Math.max(dragStart.y, currentPos.y)

    // Clear selection unless Shift is held (optional, but good UX)
    // For simplicity following prompt strictness: "выделение нескольких юнитов через зажатие лкм и выделение коробочкой"
    // I will assume box selection replaces selection for now, or adds? 
    // Usually box replaces.
    deselectAll()

    val unitPos = new Vector3()
    for (unit <- world.units) {
      unitPos.set(unit.position)
      // Project world pos to screen pos
      camera.camera.project(unitPos)
      // Screen Y is inverted in GDX input vs Camera project usually?
      // camera.project returns y with 0 at bottom. Input has 0 at top.
      // Need to flip input Y or project Y.
      // Input: 0 at top. Project: 0 at bottom.
      // Let's convert project result to input space.
      val screenY = camera.camera.viewportHeight - unitPos.y

      if (unitPos.x >= minX && unitPos.x <= maxX && 
          screenY >= minY && screenY <= maxY) {
        unit.setSelected(true)
      }
    }
  }

  private def handleLeftClick(screenX: Int, screenY: Int): Unit = {
    val ray = camera.camera.getPickRay(screenX.toFloat, screenY.toFloat)
    
    // 1. Check if we clicked a unit
    val hitUnit = world.intersectUnit(ray)
    
    if (hitUnit.isDefined) {
      // Select unit
      // If Shift held -> toggle? Prompt says "multi selection via holding LKM".
      // Assuming straightforward: Click unit -> select it.
      // To keep it simple: Deselect others unless we want to support multi-click.
      // Prompt: "выделение нескольких юнитов через зажатие лкм" -> likely means shift+click or box.
      // I'll implement: Click adds to selection if Shift is pressed? No, prompt is vague.
      // "Когда юнит выделен... На пкм ... снимается" -> Single selection logic implied mostly?
      // But "выделение нескольких..." -> Multi logic.
      
      // Implementation:
      // If simple click on unit: Select ONLY that unit.
      deselectAll()
      hitUnit.get.setSelected(true)
    } else {
      // 2. We clicked ground/world
      // Check if we have selected units
      val selectedUnits = world.units.filter(_.selected)
      
      if (selectedUnits.nonEmpty) {
        // Move command
        // Raycast world blocks
        val blockHit = world.raycast(ray, 100f)
        
        val target = new Vector3()
        var foundTarget = false

        if (blockHit.isDefined) {
          val (bx, by, bz, _) = blockHit.get
          // Move to top of block
          target.set(bx + 0.5f, by + 1f, bz + 0.5f)
          foundTarget = true
        } else {
          // Raycast ground plane as fallback
          if (Intersector.intersectRayPlane(ray, groundPlane, target)) {
            foundTarget = true
          }
        }

        if (foundTarget) {
          if (selectedUnits.size > 1) {
            // Scatter units randomly around the target
            // Radius scales with sqrt of unit count to maintain density
            val scatterRadius = Math.sqrt(selectedUnits.size).toFloat * 1.0f
            
            selectedUnits.foreach { unit =>
              val angle = Random.nextDouble() * Math.PI * 2
              // Use sqrt(random) for uniform distribution within circle, 
              // but simple random is fine for "random point" feel requested
              val dist = Random.nextDouble() * scatterRadius
              
              val offsetX = (Math.cos(angle) * dist).toFloat
              val offsetZ = (Math.sin(angle) * dist).toFloat
              
              val unitTarget = target.cpy().add(offsetX, 0f, offsetZ)
              unit.moveTo(unitTarget)
            }
          } else {
            selectedUnits.foreach(_.moveTo(target))
          }
        }
      }
    }
  }
}

