package encry.local

import akka.actor.Actor
import cats.effect.IO
import doobie.hikari.HikariTransactor
import encry.modifiers.history.block.EncryBlock
import encry.network.EncryNodeViewSynchronizer.ReceivableMessages.{SemanticallySuccessfulModifier, SyntacticallySuccessfulModifier}
import encry.utils.ScorexLogging
import encry.EncryExplorerApp.settings

import scala.concurrent.ExecutionContextExecutor

class BlockListener extends Actor with ScorexLogging {

  import doobie._   , doobie.implicits._
  import cats.data._, cats.implicits._

  implicit val ec: ExecutionContextExecutor = context.system.dispatcher

  val transactor: HikariTransactor[IO] = HikariTransactor
    .newHikariTransactor[IO](
      driverClassName = "org.postgresql.Driver",
      url = settings.postgres.host,
      user = settings.postgres.user,
      pass = settings.postgres.password
    ).map { xa => xa.configure(_ => IO(())); xa }
    .unsafeRunSync()

  override def preStart(): Unit = {
    logger.info("Start listening to new blocks.")
    context.system.eventStream.subscribe(self, classOf[SemanticallySuccessfulModifier[_]])
    context.system.eventStream.subscribe(self, classOf[SyntacticallySuccessfulModifier[_]])
  }

  override def receive: Receive = {
    case SemanticallySuccessfulModifier(block: EncryBlock) => log.debug("Got block: " + block)
  }
}
