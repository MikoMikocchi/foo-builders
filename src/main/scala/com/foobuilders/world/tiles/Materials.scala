package com.foobuilders.world.tiles

import com.badlogic.gdx.graphics.Color

object Materials {
  val Grass: MaterialDef =
    MaterialDef(
      id = MaterialId("grass"),
      displayName = "Grass",
      color = new Color(0.18f, 0.48f, 0.22f, 1.0f),
      isWalkable = true,
      isBuildable = true
    )

  val all: List[MaterialDef] =
    List(
      Grass
    )
}
