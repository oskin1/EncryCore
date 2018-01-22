package encry.view.state.index

import akka.actor.Actor
import encry.account.Address
import encry.modifiers.history.block.EncryBlock
import encry.modifiers.mempool.CoinbaseTransaction
import encry.modifiers.state.box.proposition.AddressProposition
import encry.modifiers.state.box.{AssetBox, OpenBox}
import encry.settings.{Algos, Constants, EncryAppSettings}
import encry.view.state.EncryState.indexDir
import encry.view.state.index.storage.StateIndexStorage
import io.iohk.iodb.{ByteArrayWrapper, LSMStore}
import scorex.core.ModifierId
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.utils.ScorexLogging
import scorex.crypto.authds.ADKey
import scorex.crypto.encode.Base58

import scala.collection.mutable

class StateIndexUpdater(settings: EncryAppSettings) extends Actor with ScorexLogging {

  val indexStore = new LSMStore(indexDir(settings),
    keySize = PublicKey25519Proposition.AddressLength, keepVersions = Constants.keepVersions)

  protected lazy val indexStorage = new StateIndexStorage(indexStore)

  override def receive: Receive = {

    case block: EncryBlock =>
      val stateOpsMap = mutable.HashMap.empty[Address, (mutable.Set[ADKey], mutable.Set[ADKey])]
      block.payload.transactions.foreach { tx =>
        tx.useBoxes.foreach { id =>
          stateOpsMap.get(Address @@ tx.senderProposition.address) match {
            case Some(t) =>
              if (t._2.exists(_.sameElements(id))) t._2.remove(id)
              else t._1.add(id)
            case None =>
              stateOpsMap.update(
                Address @@ tx.senderProposition.address, mutable.Set(id) -> mutable.Set.empty[ADKey])
          }
        }
        tx.newBoxes.foreach {
          case bx: AssetBox =>
            stateOpsMap.get(bx.proposition.address) match {
              case Some(t) => t._2.add(bx.id)
              case None => stateOpsMap.update(
                bx.proposition.address, mutable.Set.empty[ADKey] -> mutable.Set(bx.id))
            }
          case bx: OpenBox =>
            stateOpsMap.get(StateIndexReader.openBoxAddress) match {
              case Some(t) => t._2.add(bx.id)
              case None => stateOpsMap.update(
                Address @@ tx.senderProposition.address, mutable.Set.empty[ADKey] -> mutable.Set(bx.id))
            }
        }
      }
      bulkUpdateIndex(block.header.id, stateOpsMap)
  }

  // Updates or creates index for key `address`.
  def updateIndexFor(address: Address, toRemove: Seq[ADKey], toInsert: Seq[ADKey]): Unit = {
    val addrBytes = AddressProposition.getAddrBytes(address)
    val bxsOpt = indexStorage.boxesByAddress(address)
    bxsOpt match {
      case Some(bxs) =>
        indexStorage.update(
          ModifierId @@ Algos.hash(addrBytes ++ toRemove.head ++ toInsert.head),
          Seq(ByteArrayWrapper(addrBytes)),
          Seq(ByteArrayWrapper(addrBytes) -> ByteArrayWrapper(
            (bxs.filterNot(toRemove.contains) ++ toInsert)
              .foldLeft(Array[Byte]()) { case (buff, id) => buff ++ id }
          ))
        )
      case None =>
        indexStorage.update(
          ModifierId @@ Algos.hash(addrBytes ++ toRemove.head ++ toInsert.head),
          Seq(),
          Seq(ByteArrayWrapper(addrBytes) ->
            ByteArrayWrapper(toInsert.foldLeft(Array[Byte]()) { case (buff, id) => buff ++ id }))
        )
    }
  }

  def bulkUpdateIndex(version: ModifierId,
                      opsMap: mutable.HashMap[Address, (mutable.Set[ADKey], mutable.Set[ADKey])]): Unit = {
    val opsFinal = opsMap
      .foldLeft(Seq[(Address, Seq[ADKey])](), Seq[(Address, Seq[ADKey])]()) {
        case ((bNew, bExs), (addr, (toRem, toIns))) =>
          val bxsOpt = indexStorage.boxesByAddress(addr)
          bxsOpt match {
            case Some(bxs) =>
              bNew -> (bExs :+ (addr, bxs.filterNot(toRem.contains) ++ toIns.toSeq))
            case None =>
              (bNew :+ (addr, toIns.toSeq)) -> bExs
          }
      }
    log.info(s"Updating index for mod: ${Base58.encode(version)} ..")
    // First remove existing records assoc with addresses to be updated.
    indexStorage.update(
      ModifierId @@ Algos.hash(version),
      opsFinal._2.map(i => ByteArrayWrapper(AddressProposition.getAddrBytes(i._1))),
      Seq()
    )
    // Than insert new versions of records + new records.
    indexStorage.update(
      version,
      Seq(),
      (opsFinal._1 ++ opsFinal._2).map { case (addr, ids) =>
        ByteArrayWrapper(AddressProposition.getAddrBytes(addr)) ->
          ByteArrayWrapper(ids.foldLeft(Array[Byte]()) { case (buff, id) => buff ++ id })
      }
    )
  }
}