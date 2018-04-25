package encry.modifiers.mempool.directive

import com.google.common.primitives.{Bytes, Ints, Longs}
import encry.account.{Account, Address}
import encry.modifiers.mempool.directive.Directive.DTypeId
import encry.modifiers.state.box.proposition.DeferredProposition
import encry.modifiers.state.box.{AssetBox, EncryBaseBox}
import encry.utils.Utils
import encry.view.history.Height
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor}
import scorex.core.serialization.Serializer
import scorex.core.transaction.box.Box.Amount
import scorex.crypto.encode.Base58
import scorex.crypto.hash.Digest32

import scala.util.Try

case class CommitmentDirective(address: Address,
                               amount: Amount,
                               height: Height,
                               override val idx: Int) extends Directive {

  override type M = CommitmentDirective

  override val typeId: DTypeId = CommitmentDirective.TypeId

  override def boxes(digest: Digest32): Seq[EncryBaseBox] =
    Seq(AssetBox(DeferredProposition(address, height), Utils.nonceFromDigest(digest ++ Ints.toByteArray(idx)), amount, None))

  override val cost: Amount = 4

  override lazy val isValid: Boolean = amount > 0 && Account.validAddress(address)

  override def serializer: Serializer[M] = CommitmentDirectiveSerializer
}

object CommitmentDirective {

  val TypeId: DTypeId = 4.toByte

  implicit val jsonEncoder: Encoder[CommitmentDirective] = (d: CommitmentDirective) => Map(
    "typeId" -> d.typeId.asJson,
    "address" -> d.address.toString.asJson,
    "amount" -> d.amount.asJson,
    "heigth" -> d.height.asJson,
    "idx" -> d.idx.asJson
  ).asJson

  implicit val jsonDecoder: Decoder[CommitmentDirective] = (c: HCursor) => {
    for {
      address <- c.downField("address").as[String]
      amount <- c.downField("amount").as[Long]
      height <- c.downField("height").as[Int]
      idx <- c.downField("idx").as[Int]
    } yield {
      CommitmentDirective(
        Address @@ address,
        amount,
        Height @@ height,
        idx
      )
    }
  }
}

object CommitmentDirectiveSerializer extends Serializer[CommitmentDirective] {

  override def toBytes(obj: CommitmentDirective): Array[Byte] =
    Bytes.concat(
      Account.decodeAddress(obj.address),
      Longs.toByteArray(obj.amount),
      Ints.toByteArray(obj.height),
      Ints.toByteArray(obj.idx),
    )

  override def parseBytes(bytes: Array[Byte]): Try[CommitmentDirective] = Try {
    val address = Address @@ Base58.encode(bytes.take(Account.AddressLength))
    val amount = Longs.fromByteArray(bytes.slice(Account.AddressLength, Account.AddressLength + 8))
    val height = Height @@ Ints.fromByteArray(bytes.slice(Account.AddressLength + 8, Account.AddressLength + 8 + 4))
    val idx = Ints.fromByteArray(bytes.takeRight(4))
    CommitmentDirective(address, amount, height, idx)
  }
}
