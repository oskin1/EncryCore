package encry.view.wallet.data

import encry.account.Address
import encry.local.TestHelper
import encry.modifiers.mempool.PaymentTransaction
import encry.modifiers.state.box.AssetBox
import encry.settings.{Algos, EncryAppSettings}
import encry.view.wallet.EncryWallet
import org.scalatest.FunSuite
import scorex.core.transaction.state.PrivateKey25519Companion
import scorex.crypto.encode.Base58

class WalletDataManagerTest extends FunSuite {

  test("Wallet Data Manager Test"){

    lazy val encrySettings: EncryAppSettings = EncryAppSettings.read(Option(""))

    val wallet: EncryWallet = EncryWallet.readOrGenerate(encrySettings)

    val wdm = WalletDataManager.readOrGenerate(encrySettings, wallet)

    val factory = TestHelper
    val keys = factory.getOrGenerateKeys(factory.Props.keysFilePath)

    val validTxs = keys.map { key =>
      val proposition = key.publicImage
      val fee = factory.Props.txFee
      val timestamp = 1234567L
      val useBoxes = IndexedSeq(factory.genAssetBox(Address @@ key.publicImage.address)).map(_.id)
      val outputs = IndexedSeq((Address @@ wallet.keyStorage.keys.head.publicImage.address, factory.Props.boxValue))
      val sig = PrivateKey25519Companion.sign(
        key,
        PaymentTransaction.getMessageToSign(proposition, fee, timestamp, useBoxes, outputs)
      )
      PaymentTransaction(proposition, fee, timestamp, sig, useBoxes, outputs)
    }

    val spentTx = validTxs.map { tx =>
      val proposition = wallet.keyStorage.keys.head.publicImage
      val fee = factory.Props.txFee
      val timestamp = 1234567L
      val useBoxes = IndexedSeq(tx.newBoxes.last.id)
      val outputs = IndexedSeq((Address @@ keys.head.publicImage.address, factory.Props.boxValue))
      val sig = PrivateKey25519Companion.sign(
        wallet.keyStorage.keys.head,
        PaymentTransaction.getMessageToSign(proposition, fee, timestamp, useBoxes, outputs)
      )
      PaymentTransaction(proposition, fee, timestamp, sig, useBoxes, outputs)
    }

    val tx = validTxs :+ spentTx

    wdm.initStorage()

 //   println(Base58.encode(wdm.listOfTransactions))

    val trxCount = 50

    val spentTxCount = 25

    validTxs.slice(0, trxCount).foreach(wdm.putTx)

    spentTx.slice(0, spentTxCount).foreach(wdm.putTx)

    val expectedBalance = trxCount*factory.Props.boxValue - spentTxCount*factory.Props.boxValue

    assert(expectedBalance == wdm.getBalance, "Balance not equals")

  }

}
