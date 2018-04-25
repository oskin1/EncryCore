package encry.modifiers.state.box.proposition

import com.google.common.primitives.{Bytes, Ints}
import encry.account.{Account, AccountSerializer, Address}
import encry.modifiers.state.box.Context
import encry.modifiers.state.box.proof.Proof
import encry.view.history.Height
import io.circe.Encoder
import io.circe.syntax._
import scorex.core.serialization.Serializer

import scala.util.{Failure, Success, Try}

case class DeferredProposition(account: Account, height: Height) extends EncryProposition {

  override type M = DeferredProposition

  override def serializer: Serializer[M] = DeferredPropositionSerializer

  override def unlockTry(proof: Proof)(implicit ctx: Context): Try[Unit] =
    if (!(Account(ctx.transaction.accountPubKey.pubKeyBytes) == account && ctx.height >= height)) Failure(new Error("Unlock failed"))
    else Success()
}

object DeferredProposition {

  val TypeId: Byte = 4

  implicit val jsonEncoder: Encoder[DeferredProposition] = (p: DeferredProposition) => Map(
    "typeId" -> TypeId.toInt.asJson,
    "address" -> p.account.address.toString.asJson,
    "height" -> p.height.toInt.asJson
  ).asJson

  def apply(address: Address, height: Height): DeferredProposition = DeferredProposition(Account(address), height)
}

object DeferredPropositionSerializer extends Serializer[DeferredProposition] {

  val Length: Int = Account.AddressLength + 1 + 4

  override def toBytes(obj: DeferredProposition): Array[Byte] = Bytes.concat(
    AccountProposition.TypeId +: obj.account.bytes,
    Ints.toByteArray(obj.height)
  )

  override def parseBytes(bytes: Array[Byte]): Try[DeferredProposition] = Try {
    assert(bytes.head == AccountProposition.TypeId && bytes.tail.length + 4 == Account.AddressLength)
    AccountSerializer.parseBytes(bytes.tail.dropRight(4)).map { acc =>
      val h = Ints.fromByteArray(bytes.takeRight(4))
      DeferredProposition.apply(acc, Height @@ h)
    }.getOrElse(throw new Exception("Deserialization failed."))
  }
}
