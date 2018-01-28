package encry.view.wallet.vault

import com.google.common.primitives.Bytes
import encry.account.Address
import encry.modifiers.state.box._
import encry.modifiers.state.box.proposition.AddressProposition
import scorex.core.serialization.{BytesSerializable, Serializer}
import scorex.core.transaction.box.proposition.{Proposition, PublicKey25519Proposition}
import scorex.crypto.encode.Base58
import scorex.crypto.signatures.PublicKey

import scala.util.Try

case class WalletBox(box: AssetBox) extends BytesSerializable{

  override type M = WalletBox

  override def serializer = WalletBoxSerializer
}

object WalletBoxSerializer extends Serializer[WalletBox]{

  override def toBytes(obj: WalletBox): Array[Byte] =
    Bytes.concat(
      obj.box.serializer.toBytes(obj.box)
    )

  override def parseBytes(bytes: Array[Byte]): Try[WalletBox] = Try{
    val box = AssetBoxSerializer.parseBytes(bytes).get
    WalletBox(box)
  }
}
