package encry.client.gui.windows

import java.awt.BorderLayout
import java.awt.event.{MouseAdapter, MouseEvent}
import javax.swing._

import encry.account.Address
import encry.client.gui.GuiManager
import encry.modifiers.mempool.PaymentTransaction
import encry.modifiers.state.box.AssetBox
import encry.view.wallet.keys.KeyManager
import encry.view.wallet.storage.WalletStorage
import scorex.core.transaction.proof.Signature25519
import scorex.crypto.signatures.Curve25519

case class TransferWindow(override val guiManager: GuiManager, walletStorage: WalletStorage, keyManager: KeyManager) extends BaseWindow {

  override val windowName: String = "Transfer Window"

  val toLabel: JLabel = new JLabel("To:")

  val toField: JTextField = new JTextField()

  val amountLabel: JLabel = new JLabel("Amount:")

  val amountField: JTextField = new JTextField()

  val feeLabel: JLabel = new JLabel("Fee:")

  val feeField: JTextField = new JTextField()

  val transferButton: JButton = new JButton("Send")

  val backButton: JButton = new JButton()

  val mainPanel: JPanel = new JPanel()

  val backWindow: BaseWindow = WalletWindow(guiManager)

  override def run(frame: JFrame, parentWindow: BaseWindow): Unit = {

    frame.getContentPane.removeAll()

    backButton.setText(s"Back to: ${parentWindow.windowName}")

    mainPanel.add(backButton)

    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS))

    mainPanel.add(toLabel)

    mainPanel.add(toField)

    mainPanel.add(amountLabel)

    mainPanel.add(amountField)

    mainPanel.add(feeLabel)

    mainPanel.add(feeField)

    mainPanel.add(transferButton)

    frame.getContentPane.add(mainPanel)

    frame.setSize(400,400)

    frame.setVisible(true)

  }

  transferButton.addMouseListener(new MouseAdapter {
    override def mouseClicked(e: MouseEvent): Unit = {
      if(toField.getText.nonEmpty && amountField.getText.nonEmpty && feeField.getText.nonEmpty){
        val recepient = toField.getText()
        val amount = amountField.getText().toLong
        val proposition = keyManager.keys.head.publicImage
        val fee = feeField.getText.toLong
        val timestamp = System.currentTimeMillis
        val boxes = walletStorage.getAllBoxes.foldLeft(Seq[AssetBox]()) {
          case (seq, box) => if (seq.map(_.amount).sum < amount) seq :+ box else seq
        }
        val useBoxes = boxes.map(_.id).toIndexedSeq
        val outputs = IndexedSeq(
          (Address @@ recepient, amount),
          (Address @@ proposition.address, boxes.map(_.amount).sum - amount))
        val sig = Signature25519(Curve25519.sign(
          keyManager.keys.head.privKeyBytes,
          PaymentTransaction.getMessageToSign(proposition, fee, timestamp, useBoxes, outputs)
        ))

        guiManager.sendTxToNVH(PaymentTransaction(proposition, fee, timestamp, sig, useBoxes, outputs))
      }
    }
   }
  )

  backButton.addMouseListener(new MouseAdapter {
    override def mouseClicked(e: MouseEvent): Unit = {

      guiManager.currentWindow(backWindow)

    }
  }
  )

}
