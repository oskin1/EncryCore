package encry.view.wallet

import com.google.common.primitives.Bytes
import encry.modifiers.state.box._
import scorex.core.serialization.{BytesSerializable, Serializer}

import scala.util.Try

case class WalletBox(box: EncryBaseBox) extends BytesSerializable{

  override type M = WalletBox

  override def serializer: Serializer[M] = WalletBoxSerializer
}

object WalletBoxSerializer extends Serializer[WalletBox]{

  override def toBytes(obj: WalletBox): Array[Byte] = obj.box.typeId +: obj.box.bytes

  override def parseBytes(bytes: Array[Byte]): Try[WalletBox] = Try {
    bytes.head match {
      case AssetBox.typeId => WalletBox(AssetBoxSerializer.parseBytes(bytes.slice(1, bytes.length - 1)).get)
      case PubKeyInfoBox.typeId => WalletBox(PubKeyInfoBoxSerializer.parseBytes(bytes.slice(1, bytes.length - 1)).get)
      case _ => throw new Error(s"Unsupported box type")
    }
  }
}
