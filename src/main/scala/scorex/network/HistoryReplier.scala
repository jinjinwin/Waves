package scorex.network

import scorex.app.Application
import scorex.block.Block
import scorex.network.NetworkController.{DataFromPeer, SendToNetwork}
import scorex.network.message.Message
import scorex.utils.ScorexLogging
import scorex.network.message._

class HistoryReplier(application: Application) extends ViewSynchronizer with ScorexLogging {

  override val messageSpecs = Seq(GetSignaturesSpec, GetBlockSpec)
  protected override lazy val networkControllerRef = application.networkController

  private def history = application.blockStorage.history

  override def receive: Receive = {

    //todo: check sender and otherSigs type
    case DataFromPeer(msgId, otherSigs: Seq[Block.BlockId]@unchecked, remote)
      if msgId == GetSignaturesSpec.messageCode =>

      log.info(s"Got GetSignaturesMessage with ${otherSigs.length} sigs within")

      otherSigs.exists { parent =>
        val headers = history.blockIdsAfter(parent, application.settings.synchronizationSettings.maxChainLength)

        if (headers.nonEmpty) {
          val msg = Message(SignaturesSpec, Right(Seq(parent) ++ headers), None)
          val ss = SendToChosen(remote)
          networkControllerRef ! SendToNetwork(msg, ss)
          true
        } else false
      }

    //todo: check sender?
    case DataFromPeer(msgId, sig: Block.BlockId@unchecked, remote)
      if msgId == GetBlockSpec.messageCode =>

      history.blockById(sig).foreach { b =>
        val msg = Message(BlockMessageSpec, Right(b), None)
        val ss = SendToChosen(remote)
        networkControllerRef ! SendToNetwork(msg, ss)
      }
  }
}
