package encry.view.wallet.keys

import encry.settings.{Algos, EncryAppSettings}
import io.iohk.iodb.ByteArrayWrapper
import org.scalatest.FunSuite
import scorex.crypto.encode.Base58

class KeyManagerTest extends FunSuite {

  test("AES test"){

    lazy val encrySettings: EncryAppSettings = EncryAppSettings.read(Option(""))

    val keyManager = KeyManager.readOrGenerate(encrySettings)

    keyManager.initStorage(Algos.hash("testSeed"))

    def seed = Base58.encode(keyManager.store.get(new ByteArrayWrapper(Algos.hash("seed"))).map(_.data).getOrElse(Base58.decode("Empty").get))

    val seedBefore = seed

    keyManager.lock()

    keyManager.unlock()

    assert(seed sameElements seedBefore, "Encrypt/Decrypt failed")





  }

}
