package encry.view.wallet

import encry.account.Address
import encry.modifiers.EncryPersistentModifier
import encry.modifiers.history.block.EncryBlock
import encry.modifiers.history.block.header.EncryBlockHeader
import encry.modifiers.mempool.{CoinbaseTransaction, EncryBaseTransaction, PaymentTransaction}
import encry.modifiers.state.box.{AssetBox, EncryBaseBox}
import encry.settings.EncryAppSettings
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
                       chainTransactions: Map[ModifierId, PaymentTransaction] = Map(),
                       offchainTransactions: Map[ModifierId, PaymentTransaction] = Map(),
                       keyStorage: KeyManager)
  extends BaseWallet
  with Vault[Proposition, EncryBaseTransaction, EncryPersistentModifier, EncryWallet]
  with ScorexLogging {

  override def secretByPublicImage(publicImage: PublicKey25519Proposition): Option[PrivateKey25519] =
    keyStorage.keys.find(k => k.publicImage.address == publicImage.address)

  def historyTransactions: Seq[WalletTransaction] = chainTransactions.map(txBox => WalletTransaction(txBox._2)).toSeq

  def boxes(): Seq[WalletBox] = {
    val allBoxes = chainTransactions.foldLeft(Seq[EncryBaseBox]()){
      case (seq,txBox) =>
        seq ++
        txBox._2.newBoxes.filter(_.isInstanceOf[AssetBox])
          .filter(box =>
          secrets.map(a => a.publicImage.address).toSeq contains box.asInstanceOf[AssetBox].proposition.address).toSeq
    }
    val spentBoxes = chainTransactions.filter(tx =>
      secrets.map(a => a.publicImage.address).toSeq contains tx._2.proposition.address)
      .foldLeft(IndexedSeq[ADKey]()){
        case (seq,txBox) => seq ++ txBox._2.useBoxes
      }
    allBoxes.filter(box => !(spentBoxes contains box.id)).map(a => WalletBox(a.asInstanceOf[AssetBox]))
  }

  override def secrets: Set[PrivateKey25519] = keyStorage.keys.toSet

  override def publicKeys: Set[PublicKey25519Proposition] = secrets.foldLeft(Seq[PublicKey25519Proposition]()){
    case(set,key) => set :+ PublicKey25519Proposition(key.publicKeyBytes)
  }.toSet

  override def scanOffchain(tx: EncryBaseTransaction): EncryWallet = tx match {
    case sp: PaymentTransaction =>
      if ((secrets.map(a => a.publicKeyBytes).toSeq contains sp.proposition.bytes) || sp.createBoxes.foldRight(false) {
        (a: (Address, Amount), _: Boolean) => secrets.map(a => a.publicImage.address).toSeq contains a._1 }) {
        EncryWallet(seed, chainTransactions, offchainTransactions + (sp.id -> sp), keyStorage = this.keyStorage)
      } else this
    case ct: CoinbaseTransaction => this
  }

  override def scanOffchain(txs: Seq[EncryBaseTransaction]): EncryWallet = {
    txs.foldLeft(this) { case (wallet, tx) => wallet.scanOffchain(tx) }
  }

  override def scanPersistent(modifier: EncryPersistentModifier): EncryWallet = {
    modifier match {
      case a: EncryBlock => a.transactions.foldLeft(this) { case (w, tx) =>
        tx match {
          //TODO: not efficient
          case sp: PaymentTransaction =>
            if ((secrets.map(a => a.publicImage.address).toSeq contains sp.proposition.address) || sp.createBoxes.forall {
              a => secrets.map(a => a.publicImage.address).toSeq contains a._1
            }){
              val ct = w.chainTransactions + (sp.id -> sp)
              val oct = w.offchainTransactions - sp.id
              EncryWallet(seed, ct, oct, keyStorage = w.keyStorage)
            } else w
          case ct: CoinbaseTransaction => w
        }
      }
      case bh: EncryBlockHeader => this
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
      Map(),
      Map(),
      keyStorage = KeyManager.readOrGenerate(settings)
    )
  }
}
