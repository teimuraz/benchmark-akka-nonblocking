package com.scaltrack.benchmark

import java.util.Random

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

object WebServer {

  val rand = new Random

  def makeRequests(implicit sys: ActorSystem, mat: ActorMaterializer, ec: ExecutionContext): Future[String] = {

    val futures = (1 to 10).map( _ =>
      Http().singleRequest(HttpRequest(uri = s"http://3.17.161.135:9000/hello?p=${rand.nextInt(99999) + 1}")).map(response => response.toString())
    )

    Future.sequence(futures).map( _ => "Done")

  }

  def main(args: Array[String]) {

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher


    val route =
      path("hello") {
        get {
          val r = makeRequests

          onSuccess(r) { resp =>
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "Done"))
          }
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 9000)

    println(s"Server online at http://localhost:9000/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}

