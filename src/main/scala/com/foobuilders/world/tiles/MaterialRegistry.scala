package com.foobuilders.world.tiles

final class MaterialRegistry private (
    private val defs: Map[MaterialId, MaterialDef]
) {
  def register(defn: MaterialDef): MaterialRegistry =
    new MaterialRegistry(defs.updated(defn.id, defn))

  def get(id: MaterialId): Option[MaterialDef] =
    defs.get(id)

  def resolve(id: MaterialId): MaterialDef =
    defs.getOrElse(id, MaterialDef.unknown(id))

  def all: Iterable[MaterialDef] =
    defs.values
}

object MaterialRegistry {
  val empty: MaterialRegistry = new MaterialRegistry(Map.empty)

  val default: MaterialRegistry =
    Materials.all.foldLeft(empty) { (reg, defn) =>
      reg.register(defn)
    }
}
