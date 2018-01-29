package encry.view.wallet

import encry.account.Address
import encry.modifiers.EncryPersistentModifier
import encry.modifiers.history.block.EncryBlock
import encry.modifiers.history.block.header.EncryBlockHeader
import encry.modifiers.mempool.{CoinbaseTransaction, EncryBaseTransaction, PaymentTransaction}
import encry.modifiers.state.box.{AssetBox, EncryBaseBox}
import encry.settings.EncryAppSettings
import encry.view.wallet.data.WalletDataManager
import encry.view.wallet.keys.KeyManager
import encry.view.wallet.vault.{WalletBox, WalletTransaction}
import scorex.core.transaction.box.Box.Amount
import scorex.core.transaction.box.proposition.{Proposition, PublicKey25519Proposition}
import scorex.core.transaction.state.PrivateKey25519
import scorex.core.transaction.wallet.Vault
import scorex.core.utils.{ByteStr, ScorexLogging}
import scorex.core.{ModifierId, VersionTag}
import scorex.crypto.authds.ADKey
import scorex.crypto.encode.Base58
import scorex.crypto.signatures.Curve25519

import scala.util.{Success, Try}

case class EncryWallet(seed: ByteStr,
                       keyStorage: KeyManager)
  extends BaseWallet
  with Vault[Proposition, EncryBaseTransaction, EncryPersistentModifier, EncryWallet]
  with ScorexLogging {

  lazy val dataStorage: WalletDataManager = WalletDataManager.readOrGenerate(this)

  override def secretByPublicImage(publicImage: PublicKey25519Proposition): Option[PrivateKey25519] =
    keyStorage.keys.find(k => k.publicImage.address == publicImage.address)

  def historyTransactions: Seq[WalletTransaction] = dataStorage.getTrxs.map(WalletTransaction)

  def boxes(): Seq[WalletBox] = dataStorage.getInstOfAllBoxes.map(WalletBox)

  override def secrets: Set[PrivateKey25519] = keyStorage.keys.toSet

  override def publicKeys: Set[PublicKey25519Proposition] = secrets.foldLeft(Seq[PublicKey25519Proposition]()){
    case(set,key) => set :+ PublicKey25519Proposition(key.publicKeyBytes)
  }.toSet

  override def scanOffchain(tx: EncryBaseTransaction): EncryWallet = this


  override def scanOffchain(txs: Seq[EncryBaseTransaction]): EncryWallet = this

  override def scanPersistent(modifier: EncryPersistentModifier): EncryWallet = {
    modifier match {
      case a: EncryBlock => a.transactions.foldLeft(this) { case (w, tx) =>
        tx match {
          //TODO: not efficient
          case sp: PaymentTransaction => {
            dataStorage.putTx(sp)
            this
          }
          case ct: CoinbaseTransaction => {
            w
          }
        }
      }
      case bh: EncryBlockHeader =>
        {
          this
        }
    }
  }

  def balance: Long = boxes().foldLeft(0L)(_+_.box.amount)

  //todo: implement
  override def rollback(to: VersionTag): Try[EncryWallet] = Success(this)

  override type NVCT = this.type

}

object EncryWallet {

  def readOrGenerate(settings: EncryAppSettings): EncryWallet = {
    EncryWallet(
      ByteStr(settings.walletSettings.seed.getBytes()),
      keyStorage = KeyManager.readOrGenerate(settings)
    )
  }
}
