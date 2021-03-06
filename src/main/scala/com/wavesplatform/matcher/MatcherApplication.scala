package com.wavesplatform.matcher

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer
import com.wavesplatform.matcher.api.MatcherApiRoute
import com.wavesplatform.matcher.market.MatcherActor
import com.wavesplatform.settings.RestAPISettings
import com.wavesplatform.state2.reader.StateReader
import scorex.api.http.CompositeHttpService
import scorex.app.Application
import scorex.transaction.{BlockStorage, TransactionModule}
import scorex.utils.ScorexLogging
import scorex.wallet.Wallet

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.reflect.runtime.universe._

trait MatcherApplication extends ScorexLogging {
  def actorSystem: ActorSystem

  def matcherSettings: MatcherSettings

  def restAPISettings: RestAPISettings

  def transactionModule: TransactionModule

  def blockStorage: BlockStorage

  def wallet: Wallet

  def storedState: StateReader = blockStorage.stateReader

  lazy val matcherApiRoutes = Seq(
    MatcherApiRoute(this.asInstanceOf[Application].wallet,
      this.asInstanceOf[Application].blockStorage.stateReader,
      matcher, restAPISettings, matcherSettings)
  )

  lazy val matcherApiTypes = Seq(
    typeOf[MatcherApiRoute]
  )

  lazy val matcher: ActorRef = actorSystem.actorOf(MatcherActor.props(storedState, wallet, matcherSettings,
    transactionModule), MatcherActor.name)

  @volatile var matcherServerBinding: ServerBinding = _

  def shutdownMatcher(): Unit = {
    Await.result(matcherServerBinding.unbind(), 10.seconds)
  }

  def runMatcher() {
    log.info(s"Starting matcher on: ${matcherSettings.bindAddress}:${matcherSettings.port} ...")

    implicit val as = actorSystem
    implicit val materializer = ActorMaterializer()

    val combinedRoute = CompositeHttpService(actorSystem, matcherApiTypes, matcherApiRoutes, restAPISettings).compositeRoute
    matcherServerBinding = Await.result(Http().bindAndHandle(combinedRoute, matcherSettings.bindAddress,
      matcherSettings.port), 5.seconds)

    implicit val ec = actorSystem.dispatcher
    log.info(s"Matcher bound to ${matcherServerBinding.localAddress} ")
  }

}
