package encry.view.wallet.storage

import com.google.common.primitives.Longs
import encry.modifiers.history.block.EncryBlock
import encry.modifiers.mempool.{CoinbaseTransaction, EncryBaseTransaction, PaymentTransaction, PaymentTransactionSerializer}
import encry.modifiers.state.box.proposition.AddressProposition
import encry.modifiers.state.box._
import encry.settings.Algos
import encry.view.EncryBaseStorage
import io.iohk.iodb.{ByteArrayWrapper, Store}
import scorex.core.ModifierId
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.crypto.authds.ADKey

import scala.util.{Try}

class WalletStorage(val db: Store, val publicKeys: Set[PublicKey25519Proposition])
  extends EncryBaseStorage {

  import WalletStorage._

  def getBoxIds: Seq[ADKey] =
    getAndUnpackComplexValue(boxIdsKey, 32).map(ADKey @@ _).getOrElse(Seq())

  def getTransactionIds: Seq[ModifierId] =
    getAndUnpackComplexValue(transactionIdsKey, 32).map(ModifierId @@ _).getOrElse(Seq())

  def putBlock(block: EncryBlock): Unit = {

    val txsInDb = getTransactionIds

    val parseData = block.payload.transactions.foldLeft(Seq[ADKey](), Traversable[EncryBaseBox](), Seq[EncryBaseTransaction]()) {
      case (mainSeq, transaction) => {
        if(publicKeys contains transaction.proposition)
          (mainSeq._1 ++ transaction.useBoxes, mainSeq._2.filterNot(box => (mainSeq._1 ++ transaction.useBoxes) contains box.id), mainSeq._3 :+ transaction)
        else{
          val newBoxes = transaction.newBoxes.filter(box =>
            !box.isInstanceOf[OpenBox] &&
              publicKeys.map(a => a.address)
                .contains(box.proposition.asInstanceOf[AddressProposition].address))
          (mainSeq._1, mainSeq._2 ++ newBoxes, mainSeq._3 :+ transaction)
        }
      }
    }
    val boxesWithOutUse = getBoxIds.filterNot(key => parseData._1 == key)
    val newBoxesIds = boxesWithOutUse ++ parseData._2.map(_.id)
    val newBoxesIdsKeyValue = newBoxesIds.foldLeft(Array[Byte]())(_ ++ _)
    val newTransactionIdsKeyValue = (getTransactionIds ++ parseData._3.map(tx => tx.id)).foldLeft(Array[Byte]())(_ ++ _)
    val newBalanceKeyValue = Longs.toByteArray(
      boxesWithOutUse.map(
        boxId => getBoxById(boxId) match {
          case Some(b) => if(b.isInstanceOf[AssetBox]) b.asInstanceOf[AssetBox].amount else 0L
          case None => 0L
        }
      ).sum + parseData._2.filter(_.isInstanceOf[AssetBox]).map(_.asInstanceOf[AssetBox].amount).sum
    )
    updateWithReplacement(
      block.header.id,
      Seq(transactionIdsKey, boxIdsKey, balanceKey)
        ++ parseData._1.foldLeft(Seq[ByteArrayWrapper]()) { case (remSeq, id) => remSeq :+ ByteArrayWrapper(id) },
      Seq(
        (transactionIdsKey, ByteArrayWrapper(newTransactionIdsKeyValue)),
        (boxIdsKey, ByteArrayWrapper(newBoxesIdsKeyValue)),
        (balanceKey, ByteArrayWrapper(newBalanceKeyValue))
      ) ++ parseData._2.foldLeft(Seq[(ByteArrayWrapper, ByteArrayWrapper)]()) {
        case (seq, box) => seq :+ (ByteArrayWrapper(box.id), ByteArrayWrapper(box.typeId +: box.bytes))
      }
    )

  }

  def getBoxById(id: ADKey): Option[EncryBaseBox] = Try{
    val rawBytes = db.get(boxKeyById(id)).map(_.data).getOrElse(Array(127: Byte))
    rawBytes.head match {
      case AssetBox.typeId => AssetBoxSerializer.parseBytes(rawBytes.slice(1, rawBytes.length - 1)).get
      case PubKeyInfoBox.typeId => PubKeyInfoBoxSerializer.parseBytes(rawBytes.slice(1, rawBytes.length - 1)).get
      case 127 => throw new Error("Block doesn't found")
    }
  }.toOption

  def getAllBoxes: Seq[EncryBaseBox] =
    getBoxIds.foldLeft(Seq[EncryBaseBox]()) { case (buff, id) =>
      val bx = getBoxById(id)
      if (bx.isDefined) buff :+ bx.get else buff
    }

  def getTransactionById(id: ModifierId): Option[PaymentTransaction] = Try {
    PaymentTransactionSerializer.parseBytes(db.get(ByteArrayWrapper(id)).get.data).get
  }.toOption

}

object WalletStorage {

  val boxIdsKey = ByteArrayWrapper(Algos.hash("listOfBoxesKeys"))

  val transactionIdsKey = ByteArrayWrapper(Algos.hash("listOfTransactions"))

  val balanceKey = ByteArrayWrapper(Algos.hash("balance"))

  def boxKeyById(id: ADKey): ByteArrayWrapper = ByteArrayWrapper(id)

  def txKeyById(id: ModifierId): ByteArrayWrapper = ByteArrayWrapper(id)
}
