package encry.cli.commands

import java.security.SecureRandom

import akka.actor.ActorRef
import encry.view.history.EncryHistory
import encry.view.mempool.EncryMempool
import encry.view.state.UtxoState
import encry.view.wallet.EncryWallet
import akka.pattern._
import akka.util.Timeout
import encry.cli.Response
import encry.settings.EncryAppSettings
import encry.utils.mnemonic.Mnemonic
import scorex.core.NodeViewHolder.GetDataFromCurrentView
import scorex.crypto.encode.Base58

import scala.util.Try

object InitKeyStorage extends Command {

  override def execute(nodeViewHolderRef: ActorRef,
                       args: Array[String], settings: EncryAppSettings): Option[Response] = Try {
    implicit val timeout: Timeout = Timeout(settings.scorexSettings.restApi.timeout)
    nodeViewHolderRef ?
      GetDataFromCurrentView[EncryHistory, UtxoState, EncryWallet, EncryMempool, Unit] { view =>
        val mnemonicCode = Mnemonic.entropyToMnemonicCode(SecureRandom.getSeed(16))
        println(s"Your mnemonic code is: ${mnemonicCode}")
        view.vault.keyManager.initStorage(Mnemonic.mnemonicCodeToBytes(mnemonicCode))
      }
  }.map(_ => Some(Response("OK"))).getOrElse(Some(Response("Operation failed")))
}
