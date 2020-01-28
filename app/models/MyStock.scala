package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

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
