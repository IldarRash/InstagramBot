package com.example.instagram

import cats.effect.{Async, ConcurrentEffect, ContextShift, Sync, Timer}
import com.example.instagram.repos.impl.MessageRepoInterpreter
import com.example.instagram.services.MessageService
import doobie.util.transactor.Transactor
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.middleware.{RequestLogger, ResponseLogger}

class Routes[F[_]: Async: ConcurrentEffect: Timer: ContextShift](val xa: Transactor[F]) {
  def make : HttpApp[F] = {

    val messageInterpreter: MessageRepoInterpreter[F] = MessageRepoInterpreter(xa)
    val messageService = MessageService(messageInterpreter)
    val router = Router("/api"-> new MessageRoutes[F](messageService).routes).orNotFound


    val loggers: HttpApp[F] => HttpApp[F] = {
      { http: HttpApp[F] =>
        RequestLogger.httpApp(logHeaders = true, logBody = true)(http)
      } andThen { http: HttpApp[F] =>
        ResponseLogger.httpApp(logHeaders = true, logBody = true)(http)
      }
    }

    loggers(router)
  }
}
