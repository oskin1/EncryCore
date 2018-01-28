package encry.view.wallet.vault

import com.google.common.primitives.Bytes
import encry.modifiers.mempool.{PaymentTransaction, PaymentTransactionSerializer}
import scorex.core.serialization.{BytesSerializable, Serializer}
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.crypto.signatures.PublicKey

import scala.util.Try

case class WalletTransaction(tx: PaymentTransaction) extends BytesSerializable {
  override type M = WalletTransaction

  override def serializer = WalletTransactionSerializer

}

object WalletTransactionSerializer extends Serializer[WalletTransaction] {

  override def toBytes(obj: WalletTransaction): Array[Byte] =
    Bytes.concat(
      PaymentTransactionSerializer.toBytes(obj.tx)
    )

  override def parseBytes(bytes: Array[Byte]): Try[WalletTransaction] = Try{
    val tx = PaymentTransactionSerializer.parseBytes(bytes).get
    WalletTransaction(tx)
  }
}
