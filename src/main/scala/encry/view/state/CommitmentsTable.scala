package encry.view.state

import com.google.common.primitives.Longs
import encry.account.{Account, Address}
import encry.settings.Algos
import io.iohk.iodb.ByteArrayWrapper
import scorex.core.serialization.{BytesSerializable, Serializer}
import scorex.core.transaction.box.Box.Amount

import scala.util.Try

// TODO: Use `TreeMap` instead of `Map` here.
case class CommitmentsTable(c: Map[Address, Amount]) extends BytesSerializable {

  override type M = CommitmentsTable

  override def serializer: Serializer[M] = CommitmentsTableSerializer
}

object CommitmentsTable {

  val Key: ByteArrayWrapper = ByteArrayWrapper(Algos.hash("commitments_table"))
}

object CommitmentsTableSerializer extends Serializer[CommitmentsTable] {

  override def toBytes(obj: CommitmentsTable): Array[Byte] = obj.c.map { case (addr, am) =>
    Account.decodeAddress(addr) ++ Longs.toByteArray(am)
  }.reduce(_ ++ _)

  override def parseBytes(bytes: Array[Byte]): Try[CommitmentsTable] = Try {
    val eltLen = Account.AddressLength + 8
    assert(bytes.length % eltLen == 0)
    val records = bytes.sliding(eltLen, eltLen).map(b =>
      Account.encodeAddress(b.take(Account.AddressLength)) -> Longs.fromByteArray(b.takeRight(8)))
    CommitmentsTable(records.toMap)
  }
}
