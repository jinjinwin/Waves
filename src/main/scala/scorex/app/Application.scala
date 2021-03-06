package scorex.app

import akka.actor.ActorRef
import com.wavesplatform.settings.WavesSettings
import scorex.consensus.nxt.WavesConsensusModule
import scorex.transaction.{BlockStorage, TransactionModule}
import scorex.wallet.Wallet


trait Application {

  implicit def consensusModule: WavesConsensusModule
  implicit def transactionModule: TransactionModule

  def applicationName: String

  def appVersion: ApplicationVersion

  def blockStorage: BlockStorage

  def peerManager: ActorRef

  def networkController: ActorRef

  def coordinator: ActorRef

  def blockGenerator: ActorRef

  def blockchainSynchronizer: ActorRef

  def scoreObserver: ActorRef

  def settings: WavesSettings

  def wallet: Wallet
}
