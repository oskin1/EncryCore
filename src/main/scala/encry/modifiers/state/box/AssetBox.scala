package encry.modifiers.state.box

import com.google.common.primitives.{Bytes, Longs}
import encry.crypto.Address
import encry.modifiers.mempool.EncryTransaction.Amount
import encry.modifiers.state.box.EncryBox.BxTypeId
import encry.modifiers.state.box.proposition.AddressProposition
import encry.modifiers.state.box.serializers.SizedCompanionSerializer
import encry.settings.Algos
import io.circe.Json
import io.circe.syntax._
import scorex.core.transaction.box.proposition.PublicKey25519Proposition.AddressLength
import scorex.crypto.authds.ADKey
import scorex.crypto.encode.Base58
import scorex.crypto.hash.Digest32

import scala.util.Try

case class AssetBox(override val proposition: AddressProposition,
                    override val nonce: Long,
                    amount: Amount) extends EncryNoncedBox[AddressProposition] {

  override type M = AssetBox

  override val typeId: BxTypeId = AssetBox.typeId

  override val id: ADKey = ADKey @@ bxHash.updated(0, typeId) // 32 bytes!

  override lazy val bxHash: Digest32 = Algos.hash(
    Bytes.concat(
      proposition.bytes,
      Longs.toByteArray(nonce),
      Longs.toByteArray(amount)
    )
  )

  override def serializer: SizedCompanionSerializer[AssetBox] = AssetBoxSerializer

  override def json: Json = Map(
    "id" -> Base58.encode(id).asJson,
    "proposition" -> proposition.address.toString.asJson,
    "nonce" -> nonce.asJson,
    "value" -> value.asJson
  ).asJson
}

object AssetBox {

  val typeId: BxTypeId = 1.toByte
}

object AssetBoxSerializer extends SizedCompanionSerializer[AssetBox] {

  val Size: Int = AddressLength + 16

  override def toBytes(obj: AssetBox): Array[Byte] = {
    Bytes.concat(
      AddressProposition.getAddrBytes(obj.proposition.address),
      Longs.toByteArray(obj.nonce),
      Longs.toByteArray(obj.amount)
    )
  }

  override def parseBytes(bytes: Array[Byte]): Try[AssetBox] = Try {
    val proposition = new AddressProposition(Address @@ Base58.encode(bytes.slice(0, AddressLength)))
    val nonce = Longs.fromByteArray(bytes.slice(AddressLength, AddressLength + 8))
    val amount = Longs.fromByteArray(bytes.slice(AddressLength + 8, AddressLength + 16))
    AssetBox(proposition, nonce, amount)
  }
}