package encry.client.gui

import javax.swing.JFrame

import akka.actor.{Actor, ActorRef}
import encry.client.gui.windows.{BaseWindow, LoginWindow, WalletWindow}
import scorex.core.NodeViewHolder.GetDataFromCurrentView
import akka.pattern._
import akka.util.Timeout
import encry.modifiers.mempool.EncryBaseTransaction
import encry.settings.EncryAppSettings
import scorex.core.LocalInterface.LocallyGeneratedTransaction
import scorex.core.transaction.box.proposition.Proposition

import scala.concurrent.Future

/**
  * Class, which manages main frame
  */
case class GuiManager(settings: EncryAppSettings, nodeViewHolderRef: ActorRef) extends Actor{

  val mainFrame = new JFrame()

  mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)

  def currentWindow(window: BaseWindow) = {
    window.run(mainFrame, LoginWindow(this))
  }

  def sendTxToNVH(tx: EncryBaseTransaction) = {
    nodeViewHolderRef ! LocallyGeneratedTransaction[Proposition, EncryBaseTransaction](tx)
  }

  override def receive: Receive = {
    case startGui => currentWindow(LoginWindow(this))
  }


}

object GuiManager{
  case object startGui
}
