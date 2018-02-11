package encry.utils.mnemonic

import org.scalatest.FunSuite
import scorex.utils.Random

class MnemonicTest extends FunSuite {

  test("testMnemonicCodeToBytes$default$2") {

    val mnemonicCode = Mnemonic.entropyToMnemonicCode(Random.randomBytes(16))

    println(mnemonicCode)

  }

}
