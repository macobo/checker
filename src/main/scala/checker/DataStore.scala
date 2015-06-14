package main.scala.checker

//object TypeDefs {
//  type Field[T] = Either[T, Id]
//}

/* Usage
 *
 * case class A(b: B, c: C) extends DiffableNode
 * case class B(d: Seq[D], e: Int) extends DiffableNode
 * case class C(f: Int)
 * case class D(h: Int) extends DiffableNode
 *
 * var store = DataStore(a)
 * store = store.update(store.root.b.d(2), D(5))
 *
 * Pieces I need: How to store a map of something without losing types? Can I keep a map of [Key, Type]?
 */
//case class Id(value: String)

//trait DiffableNode
//trait SyncedDiffedNode extends DiffableNode {
//  def id: Id
//  def parents: Seq[DiffableNode]
//}
//
//class DataStore[RootType <: DiffableNode](rootNode: RootType) {
//  lazy val idMapping: Map[Id, DiffableNode] = ???
//
//  def update(id: Id, newValue: DiffableNode): DataStore[RootType] = ???
//  def getById(id: Int): DiffableNode = ???
//}
