package encry.view.wallet.keyKeeper

import akka.actor.Actor
import encry.settings.KeyKeeperSettings
import encry.view.wallet.keyKeeper.KeyKeeper.{AddKey, GetKeys, Init}
import scorex.crypto.encode.Base58

case class KeyKeeper(
                      storageSettings : KeyKeeperSettings
                    ) extends Actor{


  val keyKeeperStorage = KeyKeeperStorage.readOrGenerate(Option(System.getProperty("User.sir") + storageSettings.path),"",storageSettings)

  override def receive : Receive = {
    case AddKey =>
      println("add key")
      keyKeeperStorage.addKey()
    case Init =>
      println("Init storage")
      keyKeeperStorage.initStorage(Base58.decode("testSeed").get)
    case GetKeys =>
      println("Get keys from storage")
      keyKeeperStorage.getKeys() foreach( a => println(Base58.encode(a.publicKeyBytes)))
  }
}

object KeyKeeper {

  case object AddKey

  case object GetKeys

  case object Init
}
