package encry.cli

import akka.actor.{Actor, ActorRef}
import encry.cli.CliListener.StartListen
import encry.cli.commands.{Command, nodeEcho, nodeStop}

import scala.collection.mutable
import scala.io.StdIn._

case class CliListener(viewHolderRef: ActorRef) extends Actor{

  protected[cli] val commands: mutable.HashMap[String, mutable.HashMap[String, Command]] = mutable.HashMap.empty

  commands.update("node", mutable.HashMap(("-stop", nodeStop), ("-echo", nodeEcho)))

  override def receive: Receive = {

    case StartListen =>
      while (true) {
        val input = readLine()
        commands.get(parseCommand(input).head) match {
          case Some(value) => parseCommand(input).slice(1, parseCommand(input).length).foreach(
            command => value.get(command.split("=").head) match {
              case Some(cmd) =>
                cmd.execute(command).get
              case None =>
                throw new Error("Unsupported command")
            }
          )
          case None =>
            throw new Error("Unsupported command")
        }
      }
  }

  private def parseCommand(command: String): Seq[String] = {
    val cmds = command.split(" ").toSeq
    if(cmds.length < 2) throw new Error("Incorrect command")
    cmds
  }
}

object CliListener {

  case object StartListen
}
