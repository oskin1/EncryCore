package encry.view.wallet.keyKeeper

import java.io.File

import akka.actor.Actor
import encry.settings.KeyKeeperSettings
import io.iohk.iodb.{ByteArrayWrapper, LSMStore}
import scorex.core.transaction.state.PrivateKey25519
import scorex.core.utils.ScorexLogging
import scorex.crypto.encode.Base58
import scorex.crypto.hash.{Digest32, Sha256}
import scorex.crypto.signatures.Curve25519
import com.roundeights.hasher.Implicits._

import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

/**
  * KeyKeeperStorage manages LMStore with private keys (Only Pk25519)
 *
  * @param storage - KeyKeeperStorage storage
  * @param password - password to unlock storage
  */

case class KeyKeeperStorage(storage : LSMStore,
                            password : String,
                            storageSettings : KeyKeeperSettings = null
                           ) extends ScorexLogging{

  /**
    * Generate private key from some string bytes
    * @param seed
    * @return Key pair based on seed and chain code
    */
  //TODO: add generateFrom mnemonic key
  def generateKeyFromSeed(seed: Array[Byte]): (PrivateKey25519, Array[Byte]) = {
    val seedHashBytes = seed.sha512.bytes
    val hashSeq = seedHashBytes.sliding(32).toSeq
    val pair = Curve25519.createKeyPair(hashSeq.head)
    PrivateKey25519(pair._1, pair._2) -> hashSeq(1)
  }

  /**
    * Generate next key based on previous key
    * @param prevKey
    * @return Key pair based on previous key and chain code
    */
  def generateNextKey(prevKey: (PrivateKey25519, Array[Byte])) : (PrivateKey25519, Array[Byte]) = {
    val prevKeyHash = (prevKey._1.publicKeyBytes ++ prevKey._2).sha512.bytes
    val hashSeq = prevKeyHash.sliding(32).toSeq
    val pair = Curve25519.createKeyPair(hashSeq(0))
    PrivateKey25519(pair._1, pair._2) -> hashSeq(1)
  }


  /**
    * get hash of  Keys sequence
    * @param keysSeq
    */
  def keysHash(keysSeq: Seq[PrivateKey25519]): Digest32 = Sha256.hash(keysSeq.foldLeft(Array[Byte]()){
    case(currentHash,key) => currentHash ++ Sha256.hash(key.publicKeyBytes)
  })

  /**
    * Generate keys from seed and keysHash
    * @return Sequence of keys
    */

  private def getKeysWithFromStorageWithChainCode(): Try[Seq[(PrivateKey25519, Array[Byte])]] = Try{


    val storageKeysHash = storage.get(new ByteArrayWrapper(Sha256.hash("hash"))).get.data

    (0 to storageSettings.hashAttempts).foldLeft(Seq[(PrivateKey25519, Array[Byte])]()) {
      case (seq, _) => {
        if (storageKeysHash sameElements keysHash(seq.foldLeft(Seq[PrivateKey25519]()) { case (seq, elem) => seq :+ elem._1 })) {
          return Try(seq)
        }
        if (seq.nonEmpty) seq :+ generateNextKey(seq.last._1, seq.last._2)
        else seq :+ generateKeyFromSeed(storage.get(new ByteArrayWrapper(Sha256.hash("seed"))).get.data)
      }
    }
    throw new Error("Storage was damaged")
  }

  def getKeys(): Seq[PrivateKey25519] =
    getKeysWithFromStorageWithChainCode().get.foldLeft(Seq[PrivateKey25519]()){
      case (seq,elem) => seq :+ elem._1
    }

  /**
    * open KeyKeeperStorage and return set of keys inside store. If store dosn't exist or store was damaged return
    * only one key seq, which was generated from user-app password
    * @return
    */
  def unlock : Unit ={
    storage.get(new ByteArrayWrapper(Sha256(Base58.decode("seed").get))) match {
      case Some(date) =>

    }
  }

  //TODO: implement
  /**
    * Lock KeyKeeperStorage with GOST 34.12-2015 or AES
    */
  def lock() : Try[Unit] = Try()

  //TODO: implement
  def generateSync() : Try[Array[Byte]] = Try(Array(0.toByte))

  def addKey() : Try[Unit] = Try{
    var keys = getKeysWithFromStorageWithChainCode().get
    keys = keys :+ generateNextKey(keys.last)
    val keysWithoutChainCode = keys.foldLeft(Seq[PrivateKey25519]()){
      case (seq,elem) => seq :+ elem._1
    }
    storage.update(System.currentTimeMillis(),Seq((new ByteArrayWrapper(Sha256.hash("hash")))),Seq())
    storage.update(System.currentTimeMillis(),Seq(),Seq(
      (new ByteArrayWrapper(Sha256.hash("hash")),new ByteArrayWrapper(keysHash(keysWithoutChainCode)))
    ))
  }

  //TODO: implement
  /**
    * delete key from store
    */
  def delKey() : Try[Unit] = Try()

  def initStorage(seed: Array[Byte]): Unit = {
    println("Init storage!")
    val key = generateKeyFromSeed(seed)._1
    val hash = keysHash(Seq(key))
    storage.update(System.currentTimeMillis(),
      Seq(),
      Seq((new ByteArrayWrapper(Sha256.hash("seed")),new ByteArrayWrapper(seed)),
        (new ByteArrayWrapper(Sha256.hash("hash")),new ByteArrayWrapper(hash))
      )
    )
  }


}

object KeyKeeperStorage extends ScorexLogging{

  def readOrGenerate(filePath : Option[String], password: String, settings: KeyKeeperSettings): KeyKeeperStorage = {
    val storeFile  =  for {
      maybeFilename <- filePath
      file = new File(maybeFilename)
      if file.exists
    } yield file

    storeFile match {
      case Some(file) =>
        log.debug("KeyKepper storage exist")
        KeyKeeperStorage(new LSMStore(file,32),password,settings)
      case None =>
        log.debug("KeyKepper storage doesn't exists. Starting with empty KeyKepper storage ")
        println(System.getProperty("user.dir") + "/KeyKeeper")
        KeyKeeperStorage(new LSMStore(new File(System.getProperty("user.dir") + "/KeyKeeper")),password,settings)
    }
  }


}
