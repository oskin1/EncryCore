package encry.view.state

import encry.modifiers.mempool.EncryBaseTransaction
import encry.view.state.UtxoState.CommitmentIndices
import scorex.core.PersistentNodeViewModifier
import scorex.core.transaction._
import scorex.core.transaction.box.proposition.Proposition

import scala.util.Try

trait StateFeature

trait TransactionValidator extends StateFeature {
  def isValid(tx: EncryBaseTransaction): Boolean = validate(tx).isSuccess

  def filterValid(txs: Seq[EncryBaseTransaction]): Seq[EncryBaseTransaction] = txs.filter(isValid)

  def validate(tx: EncryBaseTransaction): Try[Option[CommitmentIndices]]
}

trait ModifierValidator[M <: PersistentNodeViewModifier] extends StateFeature {
  def validate(mod: M): Try[Unit]
}

trait BalanceSheet[P <: Proposition] extends StateFeature {
  def balance(id: P, height: Option[Int] = None): Long
}

trait AccountTransactionsHistory[P <: Proposition, TX <: Transaction[P]] extends StateFeature {
  def accountTransactions(id: P): Array[TX]
}
