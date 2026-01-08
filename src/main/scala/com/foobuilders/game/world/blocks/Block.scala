package com.foobuilders.game.world.blocks

import com.badlogic.gdx.graphics.Color

sealed trait BlockType {
  def name: String
  def color: Color
  def isSolid: Boolean
}

object BlockType {
  case object Air extends BlockType {
    val name = "Air"
    val color = Color.CLEAR
    val isSolid = false
  }

  case object Stone extends BlockType {
    val name = "Stone"
    val color = Color.GRAY
    val isSolid = true
  }
}
