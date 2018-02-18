package encry.view.mempool.transactionsAVLTree

import encry.account.Address
import encry.local.TestHelper
import encry.modifiers.mempool.{EncryBaseTransaction, PaymentTransaction}
import encry.modifiers.state.box.{EncryBaseBox, OpenBox}
import encry.settings.Algos
import org.scalatest.FunSuite
import scorex.core.transaction.state.PrivateKey25519Companion
import scorex.crypto.authds.ADKey

import scala.collection.mutable
import scala.collection.mutable.TreeSet

class TreeKeyTest extends FunSuite {

  test("TreeKey Compare") {

    val array1 = Array(2: Byte, 3: Byte, 4: Byte)

    val array2 = Array(1: Byte, 3: Byte, 5: Byte)

    val key1 = TreeKey(array1)

    val key2 = TreeKey(array2)

    assert(key1.compare(key1 ,key2) == 1, "Wrong compare")

  }

  test("Insert to MemPool container"){

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
      prevBox = tx.newBoxes.last.id
      tx
    }

    txs.foreach(tx => {
      println("----TX----")
      println("UseBox")
      tx.useBoxes.foreach(boxId => println(Algos.encode(boxId)))
      println("New Box")
      tx.newBoxes.foreach(box => println(
        box.getClass + " " +
        Algos.encode(box.id))
      )
      println()
    })

    val txsBoxes = txs.foldLeft(Seq[ADKey]()) {
      case (boxes, tx) => boxes ++ tx.newBoxes.filterNot(_.isInstanceOf[OpenBox]).map(box => box.id)
    }

    val mc = new MempoolContainer

    mc.updateTree(txs)

    assert((Seq(prevBox) diff mc.getBoxesID) == Seq.empty[ADKey], "Insert failed")

    mc.getBoxesID.foreach(a => println(Algos.encode(a)))

  }

}
