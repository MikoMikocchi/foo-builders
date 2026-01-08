package com.foobuilders.game.rendering

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.collision.Ray
import com.badlogic.gdx.math.{Intersector, Plane, Vector3}
import com.badlogic.gdx.{Gdx, Input, InputAdapter}

class GameCamera(viewportWidth: Float, viewportHeight: Float)
    extends InputAdapter {

  val camera: PerspectiveCamera =
    new PerspectiveCamera(67, viewportWidth, viewportHeight)

  camera.position.set(50f, 30f, 80f)
  camera.lookAt(50f, 0f, 50f)
  camera.near = 0.1f
  camera.far = 300f
  camera.update()

  private val moveSpeed = 5f
  private val zoomSpeed = 0.5f
  private val rotationSpeed = 0.2f
  private var targetPosition = new Vector3(camera.position)
  private var rotating = false
  private var lastX = 0f
  private var lastY = 0f

  private val groundPlane = new Plane(Vector3.Y, 0f)
  private val rotationPivot = new Vector3()
  private var hasPivot = false

  // Reusable vectors to avoid allocations
  private val forward = new Vector3()
  private val right = new Vector3()
  private val tmp = new Vector3()

  def attachInput(): Unit = {
    Gdx.input.setInputProcessor(this)
  }

  def update(delta: Float): Unit = {
    val moveDistance = moveSpeed * delta

    // Optimized logic: get X/Z direction, ignore Y, then normalize
    forward.set(camera.direction.x, 0f, camera.direction.z).nor()
    right.set(forward).crs(Vector3.Y).nor()

    if (Gdx.input.isKeyPressed(Input.Keys.W)) {
      targetPosition.add(tmp.set(forward).scl(moveDistance))
    }
    if (Gdx.input.isKeyPressed(Input.Keys.S)) {
      targetPosition.add(tmp.set(forward).scl(-moveDistance))
    }
    if (Gdx.input.isKeyPressed(Input.Keys.A)) {
      targetPosition.add(tmp.set(right).scl(-moveDistance))
    }
    if (Gdx.input.isKeyPressed(Input.Keys.D)) {
      targetPosition.add(tmp.set(right).scl(moveDistance))
    }

    camera.position.set(targetPosition)
    camera.update()
  }

  override def touchDown(
      screenX: Int,
      screenY: Int,
      pointer: Int,
      button: Int
  ): Boolean = {
    if (button == Input.Buttons.RIGHT) {
      rotating = true
      lastX = screenX.toFloat
      lastY = screenY.toFloat

      val ray = new Ray(camera.position, camera.direction)
      hasPivot = Intersector.intersectRayPlane(ray, groundPlane, rotationPivot)
      if (!hasPivot) {
        rotationPivot.set(camera.direction).scl(20f).add(camera.position)
        hasPivot = true
      }
      true
    } else {
      false
    }
  }

  override def touchUp(
      screenX: Int,
      screenY: Int,
      pointer: Int,
      button: Int
  ): Boolean = {
    if (button == Input.Buttons.RIGHT) {
      rotating = false
      true
    } else {
      false
    }
  }

  override def touchDragged(
      screenX: Int,
      screenY: Int,
      pointer: Int
  ): Boolean = {
    if (!rotating) {
      return false
    }

    val deltaX = (screenX.toFloat - lastX)
    val deltaY = (screenY.toFloat - lastY)
    lastX = screenX.toFloat
    lastY = screenY.toFloat

    val yaw = -deltaX * rotationSpeed
    val pitch = -deltaY * rotationSpeed

    if (hasPivot) {
      camera.rotateAround(rotationPivot, Vector3.Y, yaw)
      tmp.set(camera.direction).crs(camera.up).nor()
      camera.rotateAround(rotationPivot, tmp, pitch)
    } else {
      if (yaw != 0f) {
        camera.rotate(Vector3.Y, yaw)
      }

      if (pitch != 0f) {
        tmp.set(camera.direction).crs(camera.up).nor()
        if (!tmp.isZero) {
          camera.rotate(tmp, pitch)
        }
      }
    }

    camera.update()
    targetPosition.set(camera.position)
    true
  }

  override def scrolled(amountX: Float, amountY: Float): Boolean = {
    val zoomChange = amountY * zoomSpeed
    tmp.set(camera.direction).nor().scl(zoomChange)
    targetPosition.add(tmp)
    true
  }

  def resize(width: Int, height: Int): Unit = {
    camera.viewportWidth = width.toFloat
    camera.viewportHeight = height.toFloat
    camera.update()
  }
}
