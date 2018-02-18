package encry.view.mempool

import encry.modifiers.mempool.EncryBaseTransaction
import encry.view.mempool.mempoolContainer.MempoolContainer
import scorex.core.ModifierId
import scorex.core.transaction.MempoolReader

trait EncryMempoolReader extends MempoolReader[EncryBaseTransaction] {

  val container: MempoolContainer

  override def getById(id: ModifierId): Option[EncryBaseTransaction] = container.getTxBytId(id)

  override def getAll(ids: Seq[ModifierId]): Seq[EncryBaseTransaction] = container.getTxsByStrategy()

  override def contains(id: ModifierId): Boolean = container.contains(id)

  override def size: Int = container.size

  def isEmpty: Boolean = size == 0
}
