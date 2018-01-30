package encry.view.wallet.data

import java.io.File

import com.google.common.primitives.{Ints, Longs}
import encry.account.Address
import encry.modifiers.mempool.{PaymentTransaction, PaymentTransactionSerializer}
import encry.modifiers.state.box.proposition.AddressProposition
import encry.modifiers.state.box.{AssetBox, AssetBoxSerializer}
import encry.settings.{Algos, EncryAppSettings, WalletSettings}
import encry.view.wallet.EncryWallet
import io.iohk.iodb.{ByteArrayWrapper, LSMStore}
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.utils.ScorexLogging
import scorex.crypto.authds.ADKey
import scorex.crypto.encode.Base58
import scorex.crypto.hash.Digest32

import scala.util.Try

/**
  * WalletDataManager manages LMStore with wallet data, such as:
  *   - Unspent boxes
  *   - List of wallet transactions
  *   - Balance
  *
  * It keeps data in this way:
  *   Meaning of field:            In store view (Key -> Value):
  *   - List of transaction keys.  "listOfTransactions" -> TxHash1 concat TxHash2 ... concat TxHashN
  *   - List of boxes instances.   "listOfBoxesInstances" -> BoxHash1 concat BoxHash2 ... concat BoxHashN
  *   - List of boxes keys.        "listOfBoxesKeys" -> listOfBoxesInstances[1].ADKey concat listOfBoxesInstances[2].ADKey ... concat listOfBoxesInstances[N].ADKey
  *   - Transactions.              "TxHash[N]" -> TransactionN
  *   - Balance of current wallet  "Balance" -> Balance: Long
  */

case class WalletDataManager(store: LSMStore,
                             var propSeq: Seq[PublicKey25519Proposition]) extends ScorexLogging{

  /**
    * initStorage() - Initialize storage with empty fields
    */

  def initStorage(): Unit = {

    store.update(
      new ByteArrayWrapper(Algos.hash("startState")),
      Seq(),
      Seq(
        (new ByteArrayWrapper(Algos.hash("listOfTransactions")), new ByteArrayWrapper("".getBytes())),
        (new ByteArrayWrapper(Algos.hash("listOfBoxesInstances")), new ByteArrayWrapper("".getBytes())),
        (new ByteArrayWrapper(Algos.hash("listOfBoxesKeys")), new ByteArrayWrapper("".getBytes())),
        (new ByteArrayWrapper(Algos.hash("balance")), new ByteArrayWrapper(Longs.toByteArray(0L)))
      )
    )
  }

  /**
    * get value of key "listOfBoxesInstances"
    * @return
    */
  def listOfBoxesInstances: Array[Byte] = store.get(new ByteArrayWrapper(Algos.hash("listOfBoxesInstances")))
    .get.data

  /**
    * get value of key "listOfBoxesKeys"
    * @return
    */
  def listOfBoxesKeys: Array[Byte] = store.get(new ByteArrayWrapper(Algos.hash("listOfBoxesKeys")))
    .get.data

  /**
    * get value of key "listOfTransactions"
    * @return
    */
  def listOfTransactions: Array[Byte] = store.get(new ByteArrayWrapper(Algos.hash("listOfTransactions")))
    .get.data

  /**
    * Get instances of unspent boxes
    * @return unspent boxes of this wallet
    */
  def getInstOfAllBoxes: Seq[AssetBox] =
  {
    val list = listOfBoxesKeys
    (0 until (list.length/32)).foldLeft(Seq[AssetBox]())( (seq,i) =>
      seq :+ getBoxById(ADKey @@ list.slice(32*i,32*i+32)).get
    )
  }


  /**
    * Get keys of unspent boxes
    * @return seq of ADKeys
    */
  def getADKeysOfBoxes: Seq[Array[Byte]] =
  {
    val list = listOfBoxesKeys
    (0 until (list.length/32)).foldLeft(Seq[Array[Byte]]())( (seq,i) =>
      seq :+ list.slice(32*i,32*i+32)
    )
  }


  /**
    * get trxs of this wallet
    * @return
    */
  def getTrxs: Seq[PaymentTransaction] =
    listOfTransactions.sliding(1,32)
        .foldLeft(Seq[PaymentTransaction]())((seq,txHash) =>
          seq :+ PaymentTransactionSerializer.parseBytes(store.get(new ByteArrayWrapper(txHash)).get.data).get
        )

  /**
    * Update value of key "listOfBoxesKeys"
    * @param newList - new of bytes
    */
  def updateADKeysList(newList: Array[Byte]): Unit ={
    //delete previous value
    store.update(
      new ByteArrayWrapper(Algos.hash(newList ++ Longs.toByteArray(System.currentTimeMillis()))), Seq(new ByteArrayWrapper(Algos.hash("listOfBoxesKeys"))), Seq()
    )
    //put new value
    store.update(
      new ByteArrayWrapper(Algos.hash(Algos.hash(newList ++ Longs.toByteArray(System.currentTimeMillis())))), Seq(), Seq((new ByteArrayWrapper(Algos.hash("listOfBoxesKeys")), new ByteArrayWrapper(newList)))
    )
  }

  /**
    * Update value of key "listOfBoxesKeys"
    * @param newList - new list of ADkeys
    */
  def updateADKeysList(newList: Seq[ADKey]): Unit ={
    updateADKeysList(newList.foldLeft(Array[Byte]())(_ ++ _))
  }

  /**
    * Update value of key "listOfTransactions"
    * @param newList - new value
    */
  def updateTrxList(newList: Array[Byte]): Unit ={
    //delete previous value
    store.update(
      new ByteArrayWrapper(Algos.hash(newList)), Seq(new ByteArrayWrapper(Algos.hash("listOfTransactions"))), Seq()
    )
    //put new value
    store.update(
      new ByteArrayWrapper(Algos.hash(Algos.hash(newList))), Seq(), Seq((new ByteArrayWrapper(Algos.hash("listOfTransactions")), new ByteArrayWrapper(newList)))
    )
  }

  def updateBalance(newBalance: Long): Unit = {
    //delete previous value
    store.update(
      new ByteArrayWrapper(Algos.hash(Longs.toByteArray(newBalance + System.currentTimeMillis()))), Seq(new ByteArrayWrapper(Algos.hash("balance"))), Seq()
    )
    //put new value
    store.update(
      new ByteArrayWrapper(Algos.hash(Algos.hash(Longs.toByteArray(newBalance + System.currentTimeMillis())))), Seq(), Seq((new ByteArrayWrapper(Algos.hash("balance")), new ByteArrayWrapper(Longs.toByteArray(newBalance))))
    )
  }

  /**
    * Put box to store or delete it from db if it's already exist in it
    * @param box
    */

  def putBoxOrDelete(box: AssetBox): Unit = {
    if(getBoxById(box.id).isFailure)
      putBox(box)
    else
      delBoxByInst(box)
  }

  /**
    * Put box to store
    * @param box
    */
  def putBox(box: AssetBox): Unit =
      store.update(
        new ByteArrayWrapper(Algos.hash(box.id ++ Longs.toByteArray(System.currentTimeMillis()))), Seq(), Seq((new ByteArrayWrapper(Algos.hash(box.id)), new ByteArrayWrapper(AssetBoxSerializer.toBytes(box))))
      )

  /**
    * Put seq of boxes to store
    * @param boxList
    */
  def putBoxes(boxList: Seq[AssetBox]): Unit = {
    if(listOfBoxesKeys.isEmpty) {
      updateADKeysList(boxList.foldLeft(Array[Byte]())(_ ++ _.id))
    }
    else
      updateADKeysList(listOfBoxesKeys ++ boxList.foldLeft(Array[Byte]())(_ ++ _.id))
    boxList.foreach(putBoxOrDelete)
  }

  /**
    * Delete box from store by Id
    * @param id
    */
  def delBoxByID(id: ADKey): Unit = {
    updateADKeysList(listOfBoxesKeys.sliding(32).foldLeft(Array[Byte]())( (seq,key) => if(!(key sameElements id)) seq ++ key else seq))
    store.update(
      System.currentTimeMillis(),Seq(new ByteArrayWrapper(id)),Seq()
    )
  }

  /**
    * Delet box from store
    * @param box
    */
  def delBoxByInst(box: AssetBox): Unit = delBoxByID(box.id)

  /**
    * Return instance of box
    * @param id - box id
    * @return
    */
  def getBoxById(id: ADKey): Try[AssetBox] = Try {
    AssetBoxSerializer.parseBytes(store.get(new ByteArrayWrapper(Algos.hash(id))).get.data).get
  }

  def deleteBoxesById(ids: Seq[ADKey]): Unit = {
    val newList = getADKeysOfBoxes.foldLeft(Array[Byte]()) {
      case (buff, id) => if (ids.forall(!_.sameElements(id))) buff ++ id else buff }

    updateADKeysList(newList)
  }
  /**
    * Return instance of transaction
    * @param txHash - hash of transaction
    * @return
    */
  def getTxById(txHash: Digest32): Try[PaymentTransaction] = Try {
    PaymentTransactionSerializer.parseBytes(store.get(new ByteArrayWrapper(txHash)).get.data).get
  }

  /**
    * Put Tx in db
    * @param tx
    */
  def putTx(tx: PaymentTransaction): Unit = {
    if(getTxById(tx.txHash).isFailure){
      updateTrxList(listOfTransactions ++ tx.txHash)
      store.update(
        new ByteArrayWrapper(tx.txHash), Seq(), Seq((new ByteArrayWrapper(tx.txHash), new ByteArrayWrapper(PaymentTransactionSerializer.toBytes(tx))))
      )
      if(propSeq contains tx.proposition){
        deleteBoxesById(tx.useBoxes)
      }else{
        putBoxes(tx.newBoxes.filter(box => box.isInstanceOf[AssetBox] && (propSeq.map(a => new AddressProposition(Address @@ a.address)) contains box.proposition)).map(_.asInstanceOf[AssetBox]).toSeq)
      }
      refreshBalance
    } else {
      throw new Error("Tx with this txHash is already contains in db")
    }
  }

  def refreshBalance: Unit = {
    updateBalance(getInstOfAllBoxes.foldLeft(0L)(_ + _.amount))
  }

  def refreshProps(newPropList: Seq[PublicKey25519Proposition]): Unit = {
    this.propSeq = newPropList
  }

  def getBalance = Longs.fromByteArray(store.get(new ByteArrayWrapper(Algos.hash("balance"))).get.data)

  def isEmpty: Boolean = store.getAll().isEmpty

}

object WalletDataManager{

  def walletDir(settings: EncryAppSettings) = new File(settings.directory + settings.walletSettings.path)

  def readOrGenerate(settings: EncryAppSettings, wallet: EncryWallet): WalletDataManager ={
    val dir = walletDir(settings)
    dir.mkdirs()
    WalletDataManager(new LSMStore(dir, 32), wallet.keyStorage.keys.map(_.publicImage))
  }

  def readOrGenerate(wallet: EncryWallet): WalletDataManager ={
    val dir = new File(System.getProperty("user.dir") + "/encry/data/wallet")
    dir.mkdirs()
    WalletDataManager(new LSMStore(dir, 32), wallet.keyStorage.keys.map(_.publicImage))
  }

}
