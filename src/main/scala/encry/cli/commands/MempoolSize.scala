package encry.cli.commands

import akka.actor.ActorRef
import akka.util.Timeout
import encry.cli.Response
import encry.settings.EncryAppSettings
import akka.pattern._
import encry.view.history.EncryHistory
import encry.view.mempool.EncryMempool
import encry.view.state.UtxoState
import encry.view.wallet.EncryWallet
import scorex.core.NodeViewHolder.GetDataFromCurrentView

import scala.util.Try

object MempoolSize extends Command {

  override def execute(nodeViewHolderRef: ActorRef,
                       args: Array[String], settings: EncryAppSettings): Option[Response] = Try {
    implicit val timeout: Timeout = Timeout(settings.scorexSettings.restApi.timeout)
    nodeViewHolderRef ?
      GetDataFromCurrentView[EncryHistory, UtxoState, EncryWallet, EncryMempool, Unit] { view =>
        println(s"Mempool size is: ${view.pool.size} txs")
      }
  }.map(_ => Some(Response("OK"))).getOrElse(Some(Response("Operation failed")))
}
