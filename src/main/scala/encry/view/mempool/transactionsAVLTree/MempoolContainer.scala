package encry.view.mempool.transactionsAVLTree

import encry.modifiers.mempool.EncryBaseTransaction
import encry.modifiers.state.box.{EncryBaseBox, OpenBox}
import encry.settings.Algos
import scorex.core.ModifierId
import scorex.crypto.authds
import scorex.crypto.authds.ADKey

import scala.collection.mutable
import scala.collection.mutable.{TreeMap, TreeSet}

/**
  * MempoolContainer contains boxes id and associated trx.
  * BoxId (Key) -> Transaction List (Value)
  * Also stores transactions
  */
case class MempoolContainer(unspentBoxesTree: mutable.TreeMap[TreeKey, Seq[(EncryBaseTransaction, Seq[ADKey])]] = mutable.TreeMap.empty(TreeKey()),
                          transactions: Seq[EncryBaseTransaction] = Seq.empty[EncryBaseTransaction]){
  /**
    * Update MempoolContainer.
    * Delete spent boxes and invalid transactions
    * Add new boxes and transaction
    * @param toUpdate - new seq of transactions
    */
  def updateTree(toUpdate: Seq[EncryBaseTransaction]): Unit = {
    toUpdate.foreach{ tx =>
        tx.useBoxes.foreach(key => {
          val treeElem = unspentBoxesTree.get(TreeKey(key))
          treeElem match {
            case Some(value) => {
              value.foreach(container => {
                container._2.foreach(boxId => {
                  val value = unspentBoxesTree.getOrElse(TreeKey(boxId), Seq.empty)
                  if (value.nonEmpty) {
                    unspentBoxesTree.update(TreeKey(boxId), value.filterNot(box => box._1 == container._1))
                  }
                })
              })
              println(TreeKey(key) + " was deleted from tree")
              unspentBoxesTree -= TreeKey(key)
            }
            case None => println(TreeKey(key) + " doesn't exist in tree")
          }
        })
        tx.newBoxes.foreach(box => {
          if(!box.isInstanceOf[OpenBox]){
            println(TreeKey(box.id) + " was added to tree")
            unspentBoxesTree += (TreeKey(box.id) -> Seq.empty[(EncryBaseTransaction, Seq[ADKey])])
          }
        })
    }
  }
  def getBoxesID: Seq[ADKey] = unspentBoxesTree.keysIterator.map(ADKey @@ _.key).toSeq

}

case class TreeKey(key: Array[Byte] = Array[Byte](0: Byte)) extends Ordering[TreeKey]{

  override def toString: String = s"TreeKey ${Algos.encode(key)}"

  override def compare(x: TreeKey, y: TreeKey): Int ={
    val buff = x.key.indices.foldLeft(Seq[Int]()){
      case(arr, i) => arr :+ (x.key(i) compare y.key(i))
    }.filterNot(res => res == 0)
    if(buff.isEmpty) 0
    else buff.head
  }

}