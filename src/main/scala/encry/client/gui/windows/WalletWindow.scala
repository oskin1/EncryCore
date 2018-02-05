package encry.client.gui.windows

import java.awt.BorderLayout
import java.awt.event.{MouseAdapter, MouseEvent}
import javax.swing._
import javax.swing.event.TableModelListener
import javax.swing.table.TableModel

import com.google.common.primitives.Longs
import javax.swing.JTable
import javax.swing.table.TableModel

import encry.client.gui.GuiManager
import encry.modifiers.mempool.EncryBaseTransaction
import encry.view.wallet.EncryWallet
import encry.view.wallet.keys.KeyManager
import encry.view.wallet.storage.WalletStorage
import scorex.crypto.encode.Base58

case class WalletWindow(guiManager: GuiManager) extends BaseWindow {

  override val windowName: String = "Wallet frame"

  val keyManager: KeyManager = KeyManager.readOrGenerate(guiManager.settings)

  def walletStorage: WalletStorage = EncryWallet.readOrGenerate(guiManager.settings).walletStorage

  val txs: Seq[EncryBaseTransaction] = walletStorage.getAllTxs

  val transferButton: JButton = new JButton("Transfer")

  val balanceLabel: JLabel = new JLabel("Balance: " + walletStorage.getAllBoxes.map(v => v.amount).sum)

  val buttonsPanel: JPanel = new JPanel()

  val infoPanel: JPanel = new JPanel()

  val backButton: JButton = new JButton()

  val refreshButton: JButton = new JButton("Refresh Balance")

  val backWindow: BaseWindow = LoginWindow(guiManager)

  //val model = new MyTableModel(txs)

  //val table = new JTable(model)

  override def run(frame: JFrame, parentWindow: BaseWindow): Unit = {

    frame.getContentPane.removeAll()

    buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS))

    backButton.setText(s"Back to: ${parentWindow.windowName}")

    buttonsPanel.add(backButton)

    buttonsPanel.add(transferButton)

    buttonsPanel.add(refreshButton)

    infoPanel.add(balanceLabel)

    frame.getContentPane.add(BorderLayout.NORTH, buttonsPanel)

    frame.getContentPane.add(BorderLayout.CENTER, infoPanel)

    //frame.getContentPane.add(BorderLayout.SOUTH, new JScrollPane(table))

    frame.setSize(400,400)

    frame.setVisible(true)

  }

  transferButton.addMouseListener(new MouseAdapter {
    override def mouseClicked(e: MouseEvent): Unit = {

        val transferFrame: JFrame = new JFrame()

        TransferWindow(guiManager, walletStorage, keyManager).run(transferFrame, WalletWindow(guiManager))

      }
    }
  )

  refreshButton.addMouseListener(new MouseAdapter {
    override def mouseClicked(e: MouseEvent): Unit = {

      balanceLabel.setText("Balance: " + walletStorage.getAllBoxes.map(v => v.amount).sum)

    }
  }
  )



  backButton.addMouseListener(new MouseAdapter {
    override def mouseClicked(e: MouseEvent): Unit = {

      guiManager.currentWindow(backWindow)

    }
  }
  )

//  class MyTableModel(val txs: Seq[EncryBaseTransaction]) extends TableModel {
//
//    val listeners: Seq[TableModelListener] = Seq.empty[TableModelListener]
//
//    override def addTableModelListener(listener: TableModelListener): Unit = {
//      listeners :+ listener
//    }
//
//    override def getColumnClass(columnIndex: Int): Class[_] = classOf[String]
//
//    override def getColumnCount = 3
//
//    override def getColumnName(columnIndex: Int): String = {
//      columnIndex match {
//        case 0 =>
//          return "txHash"
//        case 1 =>
//          return "sender"
//        case 2 =>
//          return "amount"
//      }
//      ""
//    }
//
//    override def getRowCount: Int = txs.size
//
//    override def getValueAt(rowIndex: Int, columnIndex: Int): String = {
//      val tx = txs(rowIndex)
//      columnIndex match {
//        case 0 =>
//          return Base58.encode(tx.txHash)
//        case 1 =>
//          return {
//            if (keyManager.keys.map(_.publicImage.address) contains tx.proposition.address) "Me"
//            else tx.proposition.address
//          }
//        case 2 =>
//          return tx.newBoxes.head.value.asInstanceOf[String]
//      }
//    }
//
//    override def isCellEditable(rowIndex: Int, columnIndex: Int) = false
//
//    override def removeTableModelListener(listener: TableModelListener): Unit = {
//    }
//
//    override def setValueAt(value: Any, rowIndex: Int, columnIndex: Int): Unit = {
//    }
//  }

}


