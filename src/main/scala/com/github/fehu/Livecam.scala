package com.github.fehu

import java.awt.image.BufferedImage

import cats.effect.{ ExitCode, IO, IOApp }
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

import com.github.fehu.livecam.VideoStreamer
import com.github.fehu.livecam.gui.VideoWindow

object Livecam extends IOApp {
  val host = "192.168.100.68"
  val port = 81
  val user = "admin"
  val pwd = "654321"
  val queueSize = 100
  val imgType = BufferedImage.TYPE_3BYTE_BGR


  def run(args: List[String]): IO[ExitCode] = for {
    logger <- Slf4jLogger.create[IO]
    window <- VideoWindow.create[IO]
    _ <- logger.info("Initiating video stream")
    _ <- VideoStreamer.easyN[IO](host, port, user, pwd, queueSize)
          .resource(imgType).use {
            stream =>
              for {
                _ <- window.open
                _ <- stream.debug(logger = logger.debug(_).unsafeRunSync())
                           .interruptWhen(window.interruptSignal)
                           .evalMap(window.paintImage)
                           .compile.drain
              } yield ()
          }
  } yield ExitCode.Success
}
