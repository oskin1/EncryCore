package encry.cli.commandObjects
import encry.view.history.EncryHistory
import encry.view.mempool.EncryMempool
import encry.view.state.UtxoState
import encry.view.wallet.EncryWallet
import scorex.core.NodeViewHolder

import scala.collection.mutable
import scala.util.Try

case class appHelp(
                    commands : mutable.HashMap[String,mutable.HashMap[String, Command]]
                  ) extends Command {

  override def execute(view: NodeViewHolder.CurrentView[EncryHistory, UtxoState, EncryWallet, EncryMempool], args: Array[String]): Try[Unit] = Try {
    commands.foreach(
      blockOfCommand => {
        println(s"Comands of: ${blockOfCommand._1}")
        blockOfCommand._2.foreach(
          println(_._2.info)
        )
      }

    )
  }

}
