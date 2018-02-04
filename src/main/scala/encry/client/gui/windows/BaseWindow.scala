package encry.client.gui.windows

import javax.swing.JFrame

import akka.actor.ActorRef
import encry.client.gui.GuiManager

/**
  * Base window trait
  */
trait BaseWindow {

  val windowName: String

  val guiManager: GuiManager

  def run(frame: JFrame, parentWindow: BaseWindow): Unit

}
