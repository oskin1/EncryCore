package encry.modifiers.state.box

import encry.modifiers.mempool.EncryTransaction
import encry.modifiers.state.box.EncryBox.BxTypeId
import encry.settings.Algos
import scorex.core.transaction.box.Box
import scorex.core.transaction.box.proposition.Proposition
import scorex.crypto.authds.ADKey
import scorex.crypto.hash.Digest32

import scala.util.Try

// TODO: Should substitute `scorex.core.transaction.box.Box[P]` in the future.
trait EncryBaseBox extends Box[Proposition] {

  val typeId: BxTypeId

  val bxHash: Digest32

  override lazy val id: ADKey = ADKey @@ bxHash.updated(0, typeId) // 32 bytes!

  def unlockTry(modifier: EncryTransaction, script: Option[String], ctxOpt: Option[Context]): Try[Unit]

  override def toString: String = s"<Box type=:$typeId id=:${Algos.encode(id)}>"
}
