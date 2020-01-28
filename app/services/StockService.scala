package services

import models.MyStock
import yahoofinance.YahooFinance

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class StockService {
  def getFutureSingleQuote(symbol: String): Future[Object]
}

object StockService {
  def getFutureSingleQuote(symbol: String): Future[Object] =
    Future {
      val quoteContent: Either[String, BigDecimal] =
        try {
          Right(YahooFinance.get(symbol).getQuote(true).getPrice)
        } catch {
          case e: Exception => Left(e.getMessage)
        }
      quoteContent match {
        case Left(msg)    => msg
        case Right(price) => MyStock(symbol, price)
      }
    }
}
