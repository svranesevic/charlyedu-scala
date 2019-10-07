package io.svranesevic.charlyedu.util

import cats.effect.Concurrent
import cats.effect.Fiber

import scala.language.higherKinds

object Syntax {

  implicit final class ConcurrentSyntax[F[_]: Concurrent, A](private val fa: F[A]) {
    def start: F[Fiber[F, A]]              = Concurrent[F].start(fa)
    def race[B](fb: F[B]): F[Either[A, B]] = Concurrent[F].race(fa, fb)
  }
}
