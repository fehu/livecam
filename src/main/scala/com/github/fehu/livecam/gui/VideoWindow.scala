package com.github.fehu.livecam.gui

import java.awt.{ Graphics, GridLayout }
import java.awt.event.{ WindowAdapter, WindowEvent }
import java.awt.image.{ BufferedImage, ConvolveOp, Kernel }
import javax.swing.{ JFrame, JPanel, WindowConstants }

import cats.effect.concurrent.Ref
import cats.effect.syntax.effect._
import cats.effect.{ ConcurrentEffect, Effect }
import cats.instances.option._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import fs2.concurrent.{ Signal, SignallingRef }

class VideoWindow[F[_]](imgRef: Ref[F, Option[BufferedImage]], interrupter: SignallingRef[F, Boolean])
                       (implicit eff: Effect[F]) extends JFrame {
  import eff.delay

  def paintImage(img: BufferedImage): F[Unit] =
    imgRef.set(Some(img)) *> delay { this.repaint() }

  def open: F[Unit] = delay { this.setVisible(true) }

  def interruptSignal: Signal[F, Boolean] = interrupter

  this.setLayout(new GridLayout(1, 2))

  val imgPanel = new JPanel {
    override def paint(g: Graphics): Unit =
      imgRef.get.flatMap(_.traverse{
        img0 => delay {
          g.drawImage(img0, 0, 0, null)
        }
      }).toIO.unsafeRunSync()
  }
  this.getContentPane.add(imgPanel)

  val edgePanel = new JPanel {
    val imgOp = new ConvolveOp(new Kernel(3, 3, Array(1, 0, -1, 0, 0, 0, 1, 0, -1)))
    def imgFilter = imgOp.filter(_: BufferedImage, null)

    override def paint(g: Graphics): Unit =
      imgRef.get.flatMap(_.traverse{
        img0 => delay {
          val img = imgFilter(img0)
          g.drawImage(img, 0, 0, null)
        }
      }).toIO.unsafeRunSync()
  }
  this.getContentPane.add(edgePanel)

  this.addWindowListener(new WindowAdapter {
    override def windowClosing(e: WindowEvent): Unit = interrupter.set(true).toIO.unsafeRunSync()
  })
  this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
}

object VideoWindow {
  def create[F[_]: ConcurrentEffect]: F[VideoWindow[F]] =
    for {
      imgRef <- Ref[F].of(Option.empty[BufferedImage])
      interrupter <- SignallingRef[F, Boolean](false)
    } yield new VideoWindow(imgRef, interrupter)
}

