package encry.view.wallet.storage

import com.google.common.primitives.Longs
import encry.account.Address
import encry.consensus.Difficulty
import encry.local.TestHelper
import encry.modifiers.InstanceFactory
import encry.modifiers.history.ADProofs
import encry.modifiers.history.block.EncryBlock
import encry.modifiers.history.block.header.EncryBlockHeader
import encry.modifiers.history.block.payload.EncryBlockPayload
import encry.modifiers.mempool.PaymentTransaction
import encry.modifiers.state.box.{AssetBox, OpenBox, PubKeyInfoBox}
import encry.settings.Algos
import encry.utils.FileHelper
import io.iohk.iodb.LSMStore
import org.scalatest.FunSuite
import scorex.core.ModifierId
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.proof.Signature25519
import scorex.core.transaction.state.PrivateKey25519Companion
import scorex.crypto.authds.{ADDigest, ADKey, SerializedAdProof}
import scorex.crypto.hash.Digest32
import scorex.crypto.signatures.{PublicKey, Signature}
import scorex.utils.Random
import encry.view.wallet.storage.WalletStorage.balanceKey

class WalletStorageTest extends FunSuite {

  test("Parse block"){

    val blockHeader = EncryBlockHeader(
      99: Byte,
      new PublicKey25519Proposition(PublicKey @@ Random.randomBytes()),
      Signature25519(Signature @@ Random.randomBytes(64)),
      ModifierId @@ Random.randomBytes(),
      Digest32 @@ Random.randomBytes(),
      ADDigest @@ Random.randomBytes(33),
      Digest32 @@ Random.randomBytes(),
      99999L,
      199,
      999L,
      Difficulty @@ BigInt(999999999999999L)
    )

    val factory = TestHelper
    val k = factory.getOrGenerateKeys(factory.Props.keysFilePath).slice(0, 30)
    val keys = k.slice(0,9)
    val recepinetProp = k.slice(9,10).head

    var prevBox = IndexedSeq(factory.genAssetBox(Address @@ keys.head.publicImage.address)).map(_.id).head

    val txs = keys.map { key =>
      val proposition = key.publicImage
      val fee = factory.Props.txFee
      val timestamp = 12345678L
      val useBoxes = IndexedSeq(prevBox)
      val outputs = IndexedSeq((Address @@ recepinetProp.publicImage.address, factory.Props.boxValue))
      val sig = PrivateKey25519Companion.sign(
        key,
        PaymentTransaction.getMessageToSign(proposition, fee, timestamp, useBoxes, outputs)
      )
      val tx = PaymentTransaction(proposition, fee, timestamp, sig, useBoxes, outputs)
      prevBox = tx.newBoxes.head.id
      tx
    } //:+ InstanceFactory.addPubKeyInfoTransaction :+ InstanceFactory.coinbaseTransaction

    val rightBalance = txs.map( tx => tx.newBoxes.last match {
      case box: AssetBox => box.amount
      case _ => 0L
    }).sum

    val blockPayload = new EncryBlockPayload(ModifierId @@ Array.fill(32)(19: Byte), txs)

    val adProofs = ADProofs(ModifierId @@ Random.randomBytes(), SerializedAdProof @@ Random.randomBytes())

    val block = new EncryBlock(blockHeader,blockPayload,Option(adProofs))

    val store = new LSMStore(FileHelper.getRandomTempDir)
    val walletStorage = new WalletStorage(store, Set(recepinetProp.publicImage))

    val txsAP = walletStorage.putBlock(block)

    assert(Longs.fromByteArray(walletStorage.db.get(balanceKey).map(_.data).getOrElse(Array(0: Byte))) == rightBalance, "Balance not right")

  }

}
