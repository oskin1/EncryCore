package encry.cli.commandObjects

import akka.actor.ActorRef
import encry.view.history.EncryHistory
import encry.view.mempool.EncryMempool
import encry.view.state.UtxoState
import encry.view.wallet.EncryWallet
import scorex.core.NodeViewHolder.CurrentView

import scala.util.Try

trait Command {

  val info: String

  def execute(view: CurrentView[EncryHistory, UtxoState, EncryWallet, EncryMempool], args: Array[String]): Try[Unit]

}
