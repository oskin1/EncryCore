package encry.cli.commandObjects


import encry.view.history.EncryHistory
import encry.view.mempool.EncryMempool
import encry.view.state.UtxoState
import encry.view.wallet.EncryWallet
import encry.view.wallet.keyKeeper.KeyKeeper.AddKey
import scorex.core.NodeViewHolder
import scorex.crypto.encode.Base58

import scala.util.Try

object keyKeeperAddKey extends Command {

  override def execute(view: NodeViewHolder.CurrentView[EncryHistory, UtxoState, EncryWallet, EncryMempool], args: Array[String]): Try[Unit] = Try{
    view.vault.keyStorage.addKey()
  }

}
