package com.foobuilders.screens.world

import com.badlogic.gdx.graphics.Color

/** Configuration for distance fog effects. Designed to be swappable for day/night cycle. */
final class FogConfig(
    /** Color when looking down from above (sky/atmosphere fog) */
    val skyFogColor: Color,
    /** Color when looking up from below (underground/depth fog) */
    val depthFogColor: Color,
    /** How many levels until fog reaches maximum density */
    val fogDepth: Int,
    /** Minimum visibility at maximum fog (0.0 = invisible, 1.0 = fully visible) */
    val minVisibility: Float
) {
  def copy(
      skyFogColor: Color = this.skyFogColor,
      depthFogColor: Color = this.depthFogColor,
      fogDepth: Int = this.fogDepth,
      minVisibility: Float = this.minVisibility
  ): FogConfig = new FogConfig(skyFogColor, depthFogColor, fogDepth, minVisibility)
}

object FogConfig {

  /** Default daytime fog: light blue sky, dark underground */
  val day: FogConfig = new FogConfig(
    skyFogColor = new Color(0.6f, 0.75f, 0.9f, 1.0f),     // Light blue sky
    depthFogColor = new Color(0.08f, 0.09f, 0.12f, 1.0f), // Dark underground
    fogDepth = 12,
    minVisibility = 0.15f
  )

  /** Night fog: darker blue sky, same underground */
  val night: FogConfig = new FogConfig(
    skyFogColor = new Color(0.1f, 0.12f, 0.2f, 1.0f),     // Dark blue night sky
    depthFogColor = new Color(0.05f, 0.05f, 0.08f, 1.0f), // Even darker underground
    fogDepth = 10,
    minVisibility = 0.1f
  )
}
