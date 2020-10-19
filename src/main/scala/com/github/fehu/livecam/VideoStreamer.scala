package com.github.fehu.livecam

import java.awt.image.BufferedImage

import scala.jdk.CollectionConverters._

import cats.effect.{ ConcurrentEffect, Effect, Resource }
import cats.effect.syntax.effect._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.monadError._
import com.xuggle.mediatool.{ MediaListenerAdapter, ToolFactory }
import com.xuggle.mediatool.event.IVideoPictureEvent
import com.xuggle.xuggler.{ IContainer, IContainerFormat, IError }
import fs2.Stream
import fs2.concurrent.Queue
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

trait VideoStreamer[F[_]] {
  def resource(imgType: Int): Resource[F, Stream[F, BufferedImage]]
  def stream(imgType: Int): Stream[F, BufferedImage] = Stream.resource(resource(imgType)).flatten
}

object VideoStreamer {
  def apply[F[_]: ConcurrentEffect](url: String, codec: String, queueSize: Int): VideoStreamer[F] =
    new Impl[F](url, codec, queueSize)

  def easyN[F[_]: ConcurrentEffect](host: String, port: Int, user: String, password: String, queueSize: Int): VideoStreamer[F] =
    new Impl(EasyN.url(host, port, user, password), EasyN.codec, queueSize)

  class Impl[F[_]](url: String, codec: String, queueSize: Int)(implicit eff: ConcurrentEffect[F]) extends VideoStreamer[F] {
    import eff.delay

    private val format = IContainerFormat.getInstalledInputFormats
                                         .asScala.find(_.getInputFormatShortName == codec)
                                         .getOrElse(sys.error(s"No input format for codec '$codec'."))

    def resource(imgType: Int): Resource[F, Stream[F, BufferedImage]] =
      for {
        reader <- Resource.make(initReader)(eff delay _.close())
        queue  <- Resource liftF Queue.bounded[F, BufferedImage](queueSize)
        _      <- Resource.liftF {
                    for {
                      _ <- delay { reader.addListener(new VideoStreamer.QueuePublishingVideoListener(queue)) }
                      _ <- delay { reader.setBufferedImageTypeToGenerate(imgType) }
                      _ <- delay { reader.getContainer.open(url, IContainer.Type.READ, format) }
                      _ <- logger.info(s"container opened: ${reader.getContainer}")
                    } yield ()
                  }
        readPacket = delay { reader.readPacket() }.ensureOr(new VideoStreamer.IErrorException(_))(_ eq null)
      } yield Stream.repeatEval {
                (readPacket <* logger.debug("readPacket")) >> (queue.dequeue1 <* logger.debug("dequeue"))
              }

    private def initReader =
      for {
        container <- delay { IContainer.make() }
        reader    <- delay { ToolFactory.makeReader(container) }
      } yield reader

    private lazy val logger = Slf4jLogger.getLogger[F]
  }

  object EasyN {
    val codec = "mjpeg"

    def url(host: String, port: Int, user: String, pwd: String): String =
      s"http://$host:$port/videostream.cgi?user=$user&pwd=$pwd"
  }

  class QueuePublishingVideoListener[F[_]: Effect](queue: Queue[F, BufferedImage]) extends MediaListenerAdapter {
    private val logger = Slf4jLogger.getLogger[F]

    override def onVideoPicture(event: IVideoPictureEvent): Unit = enqueue(event.getImage).toIO.unsafeRunSync()

    private def enqueue(img: BufferedImage) = queue.enqueue1(img) *> logger.debug("enqueue")
  }

  class IErrorException(val underlying: IError) extends Exception(underlying.toString)
}
