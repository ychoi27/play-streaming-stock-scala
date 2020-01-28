package controllers

import akka.NotUsed
import akka.stream.scaladsl.Flow
import javax.inject._
import models.MyStock
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import services.StockService
import akka.util.Timeout
import scala.concurrent.duration._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents)
    extends BaseController {

  val logger = play.api.Logger(getClass)

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def getSingleQuote(symbol: String) = Action.async {
    StockService.getFutureSingleQuote(symbol).map {
      case Right(stock)  => Ok(Json.toJson(stock))
      case Left(message) => BadRequest(message)

    }
  }

  def socket: WebSocket = WebSocket.acceptOrResult[JsValue, JsValue] {
    request =>
      socketFutureFlow(request)
        .map { flow =>
          Right(flow)
        }
        .recover {
          case e: Exception =>
            logger.error("Cannot create websocket", e)
            val jsError = Json.obj("error" -> "Cannot create websocket")
            val result = InternalServerError(jsError)
            Left(result)
        }

  }

  private def socketFutureFlow(
    request: RequestHeader
  ): Future[Flow[JsValue, JsValue, NotUsed]] = {
    implicit val timeout = Timeout(1.second)
    val futureSocketFlow: Future[Any] =
      StockService.websocketFlow.mapTo[Flow[JsValue, JsValue, _]]
    val futureFlow: Future[Flow[JsValue, JsValue, NotUsed]] =
      futureSocketFlow.mapTo[Flow[JsValue, JsValue, NotUsed]]
    futureFlow
  }

}
