package encry.client.gui.windows

import java.awt.event.{MouseAdapter, MouseEvent}
import java.awt.{BorderLayout, Font}
import javax.swing._
import javax.imageio.ImageIO
import java.io.File

import encry.client.gui.GuiManager
import encry.settings.Algos
import encry.view.wallet.keys.KeyManager
import io.iohk.iodb.LSMStore
import scorex.crypto.encode.Base58

import scala.util.{Failure, Success}

case class LoginWindow(guiManager: GuiManager) extends BaseWindow {

  override val windowName = "Login window"

  val keyManager: KeyManager = KeyManager.readOrGenerate(guiManager.settings)

  //UI block start

  val font: Font = new Font("Verdana", Font.BOLD, 11)

  val logo: JLabel = new JLabel(new ImageIcon(ImageIO.read(new File(System.getProperty("user.dir") + "/src/main/resources/img/encryptotel.png"))))

  val passwordInfoLabel: JLabel = new JLabel("Password:")

  val passwordField: JPasswordField = new JPasswordField()

  val enterButton: JButton = new JButton("Enter")

  val mainPanel: JPanel = new JPanel()

  //UI block end

  override def run(frame: JFrame, parentWindow: BaseWindow): Unit = {

    passwordField.setFont(font)

    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS))

    passwordField.setVisible(true)

    mainPanel.add(logo)

    if(keyManager.isLocked) {

      mainPanel.add(passwordInfoLabel)

      mainPanel.add(passwordField)

      mainPanel.add(enterButton)
    }
    else{

      passwordInfoLabel.setText("Storage is unlocked, press 'continue' ")

      mainPanel.add(passwordInfoLabel)

      enterButton.setText("Continue")

      mainPanel.add(enterButton)

    }

    frame.getContentPane.add(BorderLayout.NORTH, mainPanel)

    frame.setSize(700,400)

    frame.setVisible(true)

  }

  enterButton.addMouseListener(new MouseAdapter {
    override def mouseClicked(e: MouseEvent): Unit = {
      if(!keyManager.isLocked){
        guiManager.currentWindow(WalletWindow(guiManager))
      }
      else if(passwordField.getPassword.nonEmpty){
        val attemptResult = keyManager.tryToUnlock(Algos.hash(passwordField.getPassword.map(_.asInstanceOf[Byte])))
        if(attemptResult){
          guiManager.currentWindow(WalletWindow(guiManager))
        } else{
          enterButton.setText("wrong")
        }
      }

    }
  })
}
