package services

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.{Done, NotUsed}
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Sink}
import models.MyStock
import play.api.libs.json.JsValue
import yahoofinance.YahooFinance

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContextExecutor, Future}

abstract class StockService {
  def getFutureSingleQuote(symbol: String): Future[Either[String, BigDecimal]]
}

object StockService {
  implicit val system: ActorSystem = ActorSystem()
  implicit val executor: ExecutionContextExecutor = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  private val stocksMap: mutable.Map[String, MyStock] = mutable.HashMap()

  def getFutureSingleQuote(symbol: String): Future[Either[String, MyStock]] =
    Future {
      val quoteContent: Either[String, MyStock] =
        try {
          val price = YahooFinance.get(symbol).getQuote(true).getPrice
          Right(MyStock(symbol, price))
        } catch {
          case _: NullPointerException => Left("NullPointerException Caught")
          case e: Exception => {
            Left(e.getMessage)
          }
        }
      quoteContent
    }

//  def getMultipleStocks(symbols: Set[String]): Future[]

  private val jsonSink: Sink[JsValue, Future[Done]] = Sink.foreach { json =>
    val symbol = (json \ "symbol").as[String]
    addStocks(Set(symbol))
  }

  def addStocks(symbols: Set[String]) =
    symbols.map(symbol => stocksMap.getOrElseUpdate(symbol, MyStock(symbol, 0)))

  val (hubSink, hubSource) = MergeHub
    .source[JsValue](perProducerBufferSize = 16)
    .toMat(BroadcastHub.sink(bufferSize = 256))(Keep.both)
    .run()

  lazy val websocketFlow: Future[Flow[JsValue, JsValue, NotUsed]] = Future {
    Flow.fromSinkAndSourceCoupled(jsonSink, hubSource)
  }
}
