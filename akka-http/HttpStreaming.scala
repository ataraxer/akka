package playground

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent.duration._


object HttpStreaming extends App {
  implicit val system = ActorSystem("http-streaming")
  implicit val flowBuilder = ActorMaterializer()
  import system.dispatcher

  val pings = Source.tick(1.seconds, 1.second, ByteString("ping"))
  val entity = HttpEntity.Chunked.fromData(ContentTypes.`text/html(UTF-8)`, pings)

  def handlerFlow(connection: Http.IncomingConnection) = {
    Flow[HttpRequest].map( _.uri.path ).map {
      case Uri.Path("/stream") =>
        HttpResponse(OK, /*Connection("close") :: */ Nil, entity)
    }.transform(() => LogState("connection"))
  }

  //val route = path("stream") & complete(result)
  //val whenConnected = Http().bindAndHandle(route, "localhost", 8080)
  val server = Http().bind("localhost", 8081)

  val handler = Sink.foreach[Http.IncomingConnection] { connection =>
    println(f"New connection: ${connection.remoteAddress}")
    connection handleWith handlerFlow(connection)
  }

  val whenConnected = server.toMat(handler)(Keep.left).run()

  val connection = Http().outgoingConnection("localhost", 8081, eagerClose = true)

  for (_ <- whenConnected) {
    Source.single(HttpRequest(uri = "/stream"))
      .via(connection)
      .flatMapConcat( _.entity.dataBytes )
      .map( _.utf8String )
      .take(3)
      .runForeach(println)
  }
}

