package encry.view.state

import com.google.common.primitives.Longs
import encry.account.{Account, Address}
import encry.settings.Algos
import scorex.core.serialization.{BytesSerializable, Serializer}
import scorex.core.transaction.box.Box.Amount

import scala.util.Try

// TODO: Use `TreeMap` instead of `Map` here.
case class CommitmentsTable(c: Map[Address, Amount]) extends BytesSerializable {

  override type M = CommitmentsTable

  override def serializer: Serializer[M] = CommitmentsTableSerializer

  def updated(toUpdate: Seq[(Address, Amount)]): CommitmentsTable =
    CommitmentsTable(toUpdate.foldLeft(Map.empty[Address, Amount]) { case (table, (addr, am)) =>
      c.get(addr)
        .map(r => table.updated(addr, r + am))
        .getOrElse(table.updated(addr, am))
    })
}

object CommitmentsTable {

  val Key: Array[Byte] = Algos.hash("commitments_table")

  def empty: CommitmentsTable = CommitmentsTable(Map.empty)
}

object CommitmentsTableSerializer extends Serializer[CommitmentsTable] {

  val Version: Byte = 99.toByte

  override def toBytes(obj: CommitmentsTable): Array[Byte] = Version +: obj.c.map { case (addr, am) =>
    Account.decodeAddress(addr) ++ Longs.toByteArray(am)
  }.foldLeft(Array.empty[Byte])(_ ++ _)

  override def parseBytes(bytes: Array[Byte]): Try[CommitmentsTable] = Try {
    val eltLen = Account.AddressLength + 8
    assert(bytes.tail.length % eltLen == 0 && bytes.head == Version)
    val records = bytes.tail.sliding(eltLen, eltLen).map(b =>
      Account.encodeAddress(b.take(Account.AddressLength)) -> Longs.fromByteArray(b.takeRight(8)))
    CommitmentsTable(records.toMap)
  }
}
