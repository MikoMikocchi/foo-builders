package com.foobuilders.world.tiles

import com.badlogic.gdx.graphics.Color

object Materials {
  val Air: MaterialDef =
    MaterialDef(
      id = MaterialId("air"),
      displayName = "Air",
      color = new Color(1.0f, 1.0f, 1.0f, 0.0f),
      isWalkable = false,
      isBuildable = false,
      blocksSight = false
    )

  val Dirt: MaterialDef =
    MaterialDef(
      id = MaterialId("dirt"),
      displayName = "Dirt",
      color = new Color(0.45f, 0.33f, 0.20f, 1.0f),
      isWalkable = true,
      isBuildable = true,
      blocksSight = true
    )

  val Grass: MaterialDef =
    MaterialDef(
      id = MaterialId("grass"),
      displayName = "Grass",
      color = new Color(0.18f, 0.48f, 0.22f, 1.0f),
      isWalkable = true,
      isBuildable = true,
      blocksSight = true
    )

  val all: List[MaterialDef] =
    List(
      Air,
      Dirt,
      Grass
    )
}
