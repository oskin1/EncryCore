package encry.view.wallet

import java.io.File

import encry.VersionTag
import encry.modifiers.EncryPersistentModifier
import encry.modifiers.mempool.EncryBaseTransaction
import encry.modifiers.state.box.proposition.EncryProposition
import encry.settings.EncryAppSettings
import encry.utils.ScorexLogging

import scala.util.{Success, Try}

class EncryWallet extends Vault[EncryProposition, EncryBaseTransaction, EncryPersistentModifier, EncryWallet] with ScorexLogging {

  override def scanOffchain(tx: EncryBaseTransaction): EncryWallet = this

  override def scanOffchain(txs: Seq[EncryBaseTransaction]): EncryWallet = this

  override def scanPersistent(modifier: EncryPersistentModifier): EncryWallet = this

  override def rollback(to: VersionTag): Try[EncryWallet] = Success(this)
}

object EncryWallet {

  def getWalletDir(settings: EncryAppSettings): File = new File(s"${settings.directory}/wallet")

  def readOrGenerate: EncryWallet = new EncryWallet
}