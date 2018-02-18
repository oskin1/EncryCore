package encry.view.mempool.mempoolContainer

import encry.modifiers.mempool.EncryBaseTransaction
import encry.modifiers.state.box.OpenBox
import encry.settings.{Algos, NodeSettings}
import scorex.core.ModifierId
import scorex.core.utils.ScorexLogging
import scorex.crypto.authds.ADKey

import scala.collection.mutable

/**
  * MempoolContainer contains boxes id and associated trx.
  * BoxId (Key) -> Transaction List (Value)
  * Also stores transactions
  */
case class MempoolContainer(unspentBoxesTree: mutable.TreeMap[TreeKey, Seq[EncryBaseTransaction]] = mutable.TreeMap.empty(TreeKey()),
                          transactions: mutable.TreeMap[TreeKey, (EncryBaseTransaction, IndexedSeq[ADKey])] = mutable.TreeMap.empty(TreeKey()),
                            settings: NodeSettings) extends ScorexLogging{
  /**
    * Update MempoolContainer when new block has been received
    * Delete spent boxes and invalid transactions
    * Add new boxes and transaction
    * @param toUpdate - seq of block transactions
    */
  def updateTree(toUpdate: Seq[EncryBaseTransaction]): Unit = {
    toUpdate.foreach{ tx =>
        tx.useBoxes.foreach(key => {
          deleteBoxFromTree(key)
        })
        tx.newBoxes.foreach(box => {
          if(!box.isInstanceOf[OpenBox]){
            unspentBoxesTree += (TreeKey(box.id) -> Seq.empty[EncryBaseTransaction])
          }
        })
    }
  }

  def addTx(tx: EncryBaseTransaction): Unit = addTxs(Seq(tx))

  /**
    * Add txs seq to transactions(Tree) and add useBoxes to unspentBoxesTree
    * @param txs
    */
  def addTxs(txs: Seq[EncryBaseTransaction]): Unit = txs.foreach(tx => {
    if(transactions.size < settings.mempoolMaxCapacity) {
      transactions += (TreeKey(tx.id) -> (tx, tx.useBoxes))
      tx.useBoxes.foreach(key => {
        addOrRefBox(key, tx)
      })
    }
  })

  def size: Int = transactions.size

  /**
    * Delete tx from unspentBoxesTree and transactions(Tree)
    * @param tx
    */
  def delTx(tx: EncryBaseTransaction): Unit = {
    transactions.get(TreeKey(tx.id)) match {
      case Some(value) => {
        value._2.foreach(aDKey =>
          unspentBoxesTree.get(TreeKey(aDKey)) match {
            case Some(value) => unspentBoxesTree.update(TreeKey(aDKey), value diff Seq(tx))
            case None => case None => log.info(s"Box ${Algos.encode(aDKey)} doesn't exist in mempool container")
        })
      }
    }
    transactions -= TreeKey(tx.id)
  }

  /**
    * Delete box from unspentBoxesTree and invalid txs
    * @param aDKey
    */
  def deleteBoxFromTree(aDKey: ADKey): Unit =
    unspentBoxesTree.get(TreeKey(aDKey)) match {
      case Some(value) => {
        value.foreach(delTx)
        unspentBoxesTree -= TreeKey(aDKey)
      }
      case None => log.info(s"Box ${Algos.encode(aDKey)} doesn't exist in mempool container")
    }

  /**
    * Add box to unspentBoxesTree, or, if it's exist update value
    * @param aDKey
    * @param tx
    */
  def addOrRefBox(aDKey: ADKey, tx: EncryBaseTransaction): Unit = {
    unspentBoxesTree.get(TreeKey(aDKey)) match {
      case Some(value) => {
        unspentBoxesTree.update(TreeKey(aDKey), value ++ Seq(tx))
      }
      case None =>{
        unspentBoxesTree += (TreeKey(aDKey) -> Seq(tx))
      }
    }
  }

  def contains(id: ModifierId): Boolean = {
    transactions.contains(TreeKey(id))
  }

  def getTxBytId(id: ModifierId): Option[EncryBaseTransaction] = transactions.get(TreeKey(id)).map(_._1)

  def getBoxesID: Seq[ADKey] = unspentBoxesTree.keysIterator.map(ADKey @@ _.key).toSeq

  /**
    * Getting txs from mempool
    */
  //TODO: implement strategy
  def getTxsByStrategy(count: Int = transactions.size): Seq[EncryBaseTransaction] = {
    val rightBorder = if(count > transactions.size) transactions.size else count
    transactions.keys.slice(0, rightBorder).toSeq.foldLeft(Seq[EncryBaseTransaction]()) {
      case (seq, key) => {
        val tx = transactions.get(key) match {
          case Some(v) => Seq(v._1)
          case None => Seq()
        }
        seq ++ tx
      }
    }
  }
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