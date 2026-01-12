package com.foobuilders.world.tiles

opaque type MaterialId = String

object MaterialId {
  def apply(value: String): MaterialId = value

  extension (id: MaterialId) {
    def value: String = id
  }
}
