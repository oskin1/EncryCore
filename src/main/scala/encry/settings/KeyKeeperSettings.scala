package encry.settings

case class KeyKeeperSettings(
                              path: String,
                              lock : Boolean,
                              cypherAlgorithm : String,
                              hashAttempts : Int,
                              keyUnlockAttempts : Int
                            )
