package scorex.transaction

import scorex.account.Account
import scorex.block.Block
import scorex.block.Block.BlockId
import scorex.crypto.encode.Base58
import scorex.network.Checkpoint

import scala.util.Try
import scorex.transaction.History.BlockchainScore

trait History {

  def height(): Int

  def blockAt(height: Int): Option[Block]

  def score(): BlockchainScore

  def scoreOf(id: BlockId): BlockchainScore

  def heightOf(blockId: BlockId): Option[Int]

  def generatedBy(account: Account, from: Int, to: Int): Seq[Block]

  def lastBlockIds(howMany: Int): Seq[BlockId]
}

trait HistoryWriter {
  def appendBlock(block: Block): Either[ValidationError, Unit]

  def discardBlock(): Unit
}

trait CheckpointService {

  def set(checkpoint: Option[Checkpoint])

  def get: Option[Checkpoint]
}


object History {
  type BlockchainScore = BigInt

  implicit class HistoryExt(history: History) {
    def isEmpty: Boolean = history.height() == 0

    def contains(block: Block): Boolean = history.contains(block.uniqueId)

    def contains(signature: Array[Byte]): Boolean = history.heightOf(signature).isDefined

    def blockById(blockId: BlockId): Option[Block] = history.heightOf(blockId).flatMap(history.blockAt)

    def blockById(blockId: String): Option[Block] = Base58.decode(blockId).toOption.flatMap(history.blockById)

    def heightOf(block: Block): Option[Int] = history.heightOf(block.uniqueId)

    def confirmations(block: Block): Option[Int] =
      heightOf(block).map(history.height() - _)

    def lastBlock: Block = history.blockAt(history.height()).get

    def averageDelay(block: Block, blockNum: Int): Try[Long] = Try {
      (block.timestampField.value - parent(block, blockNum).get.timestampField.value) / blockNum
    }

    def parent(block: Block, back: Int = 1): Option[Block] = {
      require(back > 0)
      history.heightOf(block.referenceField.value).flatMap(referenceHeight => history.blockAt(referenceHeight - back + 1))
    }

    def child(block: Block): Option[Block] = history.heightOf(block.uniqueId).flatMap(h => history.blockAt(h + 1))

    def lastBlocks(howMany: Int): Seq[Block] =
      (Math.max(1, history.height() - howMany + 1) to history.height()).flatMap(history.blockAt).reverse

    def blockIdsAfter(parentSignature: BlockId, howMany: Int): Seq[BlockId] =
      history.heightOf(parentSignature).map { h =>
        (h + 1).to(Math.min(history.height(), h + howMany: Int)).flatMap(history.blockAt).map(_.uniqueId)
      }.getOrElse(Seq())

    def genesis: Block = history.blockAt(1).get
  }

}
