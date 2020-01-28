package controllers

import javax.inject._
import models.MyStock
import play.api.libs.json.Json
import play.api.mvc._
import services.StockService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents)
    extends BaseController {

  /**
    * Create an Action to render an HTML page.
    *
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def getSingleQuote(symbol: String) = Action.async {
    StockService.getFutureSingleQuote(symbol).map {
      case Right(price)       => Ok(Json.toJson(MyStock(symbol, price)))
      case Left(errorMessage) => BadRequest(errorMessage)
    }
  }

}
