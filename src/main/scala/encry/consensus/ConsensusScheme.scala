package encry.consensus

import encry.modifiers.history.block.EncryBlock
import encry.modifiers.history.block.header.EncryBlockHeader
import encry.modifiers.mempool.EncryBaseTransaction
import encry.ModifierId
import scorex.crypto.authds.SerializedAdProof
import scorex.crypto.hash.Digest32

import scala.math.BigInt

trait ConsensusScheme {

  val difficultyController: PowLinearController.type = PowLinearController

  def verifyCandidate(candidateBlock: CandidateBlock, nonce: Long): Option[EncryBlock] =
    verifyCandidate(candidateBlock, nonce, nonce)

  def verifyCandidate(candidateBlock: CandidateBlock, finishingNonce: Long, startingNonce: Long): Option[EncryBlock]

  def realDifficulty(header: EncryBlockHeader): BigInt

  def getDerivedHeaderFields(parentOpt: Option[EncryBlockHeader], adProofBytes: SerializedAdProof,
                             transactions: Seq[EncryBaseTransaction]): (Byte, ModifierId, Digest32, Digest32, Int)

  def correctWorkDone(realDifficulty: Difficulty, difficulty: BigInt): Boolean = {
    realDifficulty >= difficulty
  }
}