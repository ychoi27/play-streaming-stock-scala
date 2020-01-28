package models

import akka.NotUsed
import akka.stream.ThrottleMode
import akka.stream.scaladsl.Source
import play.api.libs.functional.syntax._
import play.api.libs.json._
import yahoofinance.YahooFinance

import scala.concurrent.duration._

case class Stock(symbol: String) {
  private val source =
    Source.unfold(
      MyStock(symbol, YahooFinance.get(symbol).getQuote(true).getPrice)
    ) { last: MyStock =>
      val next =
        MyStock(symbol, YahooFinance.get(symbol).getQuote(true).getPrice)
      Some(next, next)
    }

  def update: Source[StockUpdate, NotUsed] = {
    source
      .throttle(
        elements = 1,
        per = 75.millis,
        maximumBurst = 1,
        ThrottleMode.shaping
      )
      .map { sq =>
        new StockUpdate(sq.symbol, sq.price)
      }
  }
}

case class MyStock(symbol: String, price: BigDecimal)

object MyStock {
  implicit val locationWrites = new Writes[MyStock] {
    def writes(stock: MyStock) =
      Json.obj("symbol" -> stock.symbol, "price" -> stock.price)
  }

  implicit val residentReads: Reads[MyStock] = (
    (JsPath \ "symbol").read[String] and
      (JsPath \ "price").read[BigDecimal]
  )(MyStock.apply _)
}

case class StockUpdate(symbol: String, price: BigDecimal)

object StockUpdate {
  import play.api.libs.json._ // Combinator syntax

  implicit val stockUpdateWrites: Writes[StockUpdate] =
    (update: StockUpdate) =>
      Json.obj(
        "type" -> "addStock",
        "symbol" -> update.symbol,
        "price" -> update.price
    )
}
