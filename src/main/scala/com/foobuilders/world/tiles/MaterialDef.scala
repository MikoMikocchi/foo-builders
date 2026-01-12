package com.foobuilders.world.tiles

import com.badlogic.gdx.graphics.Color

final case class MaterialDef(
    id: MaterialId,
    displayName: String,
    color: Color,
    isWalkable: Boolean = true,
    isBuildable: Boolean = true,
    blocksSight: Boolean = false
)

object MaterialDef {
  def unknown(id: MaterialId): MaterialDef =
    MaterialDef(
      id = id,
      displayName = s"Unknown(${id.value})",
      color = new Color(1.0f, 0.0f, 1.0f, 1.0f),
      isWalkable = false,
      isBuildable = false,
      blocksSight = true
    )
}
