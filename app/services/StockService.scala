package services

import akka.actor.ActorSystem
import akka.stream.{
  ActorMaterializer,
  KillSwitches,
  Materializer,
  UniqueKillSwitch
}
import akka.{Done, NotUsed}
import akka.stream.scaladsl.{
  BroadcastHub,
  Flow,
  Keep,
  MergeHub,
  RunnableGraph,
  Sink
}
import models.{MyStock, Stock}
import play.api.libs.json.{JsValue, Json}
import yahoofinance.YahooFinance

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}

abstract class StockService {
  def getFutureSingleQuote(symbol: String): Future[Either[String, BigDecimal]]
}

object StockService {
  implicit val system: ActorSystem = ActorSystem()
  implicit val executor: ExecutionContextExecutor = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  private val stocksSwitchMap: mutable.Map[String, UniqueKillSwitch] =
    mutable.HashMap()

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

  private val jsonSink: Sink[JsValue, Future[Done]] = Sink.foreach { json =>
    val symbol = (json \ "symbol").as[String]
    addStocks(Set(symbol))
  }

  def addStocks(symbols: Set[String]) = Future[Unit] {
    println("add Stocks " + symbols)
    val future: Future[Set[Stock]] =
      Future(symbols.map(symbol => Stock(symbol)))

//    println(future)

    future.map { (newStocks: Set[Stock]) =>
      newStocks.foreach { stock =>
//        if (!stocksSwitchMap.contains(stock.symbol)) {
        println("added Symbol " + stock.symbol + " with update " + stock.update)
        addStock(stock)
//        }
      }
    }
  }

  private def addStock(stock: Stock): Unit = {
    val stockSource = stock.update.map(su => Json.toJson(su))
    println("stockSource " + stockSource)

    val killswitchFlow: Flow[JsValue, JsValue, UniqueKillSwitch] = {
      Flow
        .apply[JsValue]
        .joinMat(KillSwitches.singleBidi[JsValue, JsValue])(Keep.right)
        .backpressureTimeout(1.seconds)
    }

    // Set up a complete runnable graph from the stock source to the hub's sink
    val graph: RunnableGraph[UniqueKillSwitch] = {
      stockSource
        .viaMat(killswitchFlow)(Keep.right)
        .to(hubSink)
        .named(s"stock-${stock.symbol}")
    }

    // Start it up!
    val killSwitch = graph.run()

    // Pull out the kill switch so we can stop it when we want to unwatch a stock.
    stocksSwitchMap += (stock.symbol -> killSwitch)
  }

  val (hubSink, hubSource) = MergeHub
    .source[JsValue](perProducerBufferSize = 16)
    .toMat(BroadcastHub.sink(bufferSize = 256))(Keep.both)
    .run()

  lazy val websocketFlow: Future[Flow[JsValue, JsValue, NotUsed]] = Future {
    Flow.fromSinkAndSourceCoupled(jsonSink, hubSource)
  }

  def unwatchStocks(symbols: Set[String]): Unit = {
    symbols.foreach { symbol =>
      stocksSwitchMap.get(symbol).foreach { killSwitch =>
        killSwitch.shutdown()
      }
      stocksSwitchMap -= symbol
    }
  }
}
