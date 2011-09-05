package scalaz

import Kleisli._

sealed trait ReaderWriterStateT[R, W, S, F[_], A] {
  val runT: R => S => F[(A, S, W)]

  def apply(r: R, s: S): F[(A, S, W)] =
    runT(r)(s)

  import =~~=._

  def *->* : (({type λ[α] = ReaderWriterStateT[R, W, S, F, α]})#λ *->* A) =
    scalaz.*->*.!**->**![({type λ[α] = ReaderWriterStateT[R, W, S, F, α]})#λ, A](this)

  import ReaderWriterStateT._

  def run(r: R, s: S)(implicit i: F =~~= Identity): (A, S, W) =
    runT(r)(s)

  def rT(r: R, s: S)(implicit ftr: Functor[F]): F[A] =
    ftr.fmap((asw: (A, S, W)) => asw._1)(runT(r)(s))

  def r(r: R, s: S)(implicit i: F =~~= Identity): A =
    rT(r, s)(new Functor[F] {
        def fmap[A, B](f: A => B) =
          k => <=~~[F, B](f(~~=>(k)(i)))
      })

  def sT(r: R, s: S)(implicit ftr: Functor[F]): F[S] =
    ftr.fmap((asw: (A, S, W)) => asw._2)(runT(r)(s))

  def s(r: R, s: S)(implicit i: F =~~= Identity): S =
    sT(r, s)(new Functor[F] {
        def fmap[A, B](f: A => B) =
          k => <=~~[F, B](f(~~=>(k)(i)))
      })

  def wT(r: R, s: S)(implicit ftr: Functor[F]): F[W] =
    ftr.fmap((asw: (A, S, W)) => asw._3)(runT(r)(s))

  def w(r: R, s: S)(implicit i: F =~~= Identity): W =
    wT(r, s)(new Functor[F] {
        def fmap[A, B](f: A => B) =
          k => <=~~[F, B](f(~~=>(k)(i)))
      })

  def state(r: R)(implicit ftr: Functor[F]): StateT[S, F, A] =
    StateT.stateT((s: S) => ftr.fmap((asw: (A, S, W)) => (asw._1, asw._2))(runT(r)(s)))

  def rsw(implicit ftr: Functor[F]): ReaderT[R, ({type λ[α] = StateT[S, ({type λ[α] = WriterT[W, F, α]})#λ, α]})#λ, A] =
    Kleisli.kleisli[R, ({type λ[α] = StateT[S, ({type λ[α] = WriterT[W, F, α]})#λ, α]})#λ, A](r =>
      StateT.stateT[S, ({type λ[α] = WriterT[W, F, α]})#λ, A](s =>
        WriterT.writerT[W, F, (A, S)](implicitly[Functor[F]].fmap((asw: (A, S, W)) => (asw._3, (asw._1, asw._2)))(runT(r)(s)))))

  def rws(implicit ftr: Functor[F]): ReaderT[R, ({type λ[α] = WriterT[W, ({type λ[α] = StateT[S, F, α]})#λ, α]})#λ, A] =
    Kleisli.kleisli[R, ({type λ[α] = WriterT[W, ({type λ[α] = StateT[S, F, α]})#λ, α]})#λ, A](r =>
      WriterT.writerT[W, ({type λ[α] = StateT[S, F, α]})#λ, A](
        StateT.stateT[S, F, (W, A)](s => implicitly[Functor[F]].fmap((asw: (A, S, W)) => ((asw._3, asw._1), asw._2))(runT(r)(s)))))

  def evalT(r: R, s: S)(implicit ftr: Functor[F]): F[(A, W)] =
    ftr.fmap((asw: (A, S, W)) => (asw._1, asw._3))(runT(r)(s))

  def eval(r: R, s: S)(implicit i: F =~~= Identity): (A, W) =
    evalT(r, s)(new Functor[F] {
        def fmap[A, B](f: A => B) =
          k => <=~~[F, B](f(~~=>(k)(i)))
      })

  def exec(r: R)(implicit ftr: Functor[F]): StateT[S, F, W] =
    StateT.stateT((s: S) => ftr.fmap((asw: (A, S, W)) => (asw._3, asw._2))(runT(r)(s)))

  def map[B](f: A => B)(implicit ftr: Functor[F]): ReaderWriterStateT[R, W, S, F, B] =
    readerWriterStateT(r => s => implicitly[Functor[F]].fmap((asw: (A, S, W)) => (f(asw._1), asw._2, asw._3))(runT(r)(s)))

  def flatMap[B](f: A => ReaderWriterStateT[R, W, S, F, B])(implicit m: BindFunctor[F], sg: Semigroup[W]): ReaderWriterStateT[R, W, S, F, B] =
    readerWriterStateT(r => s => m.bd((asw: (A, S, W)) => m.fmap((bsw: (B, S, W)) => (bsw._1, bsw._2, sg.append(asw._3, bsw._3)))(f(asw._1).runT(r)(asw._2)))(runT(r)(s)))
}

object ReaderWriterStateT extends ReaderWriterStateTs {
  def apply[R, W, S, F[_], A](k: R => S => F[(A, S, W)]): ReaderWriterStateT[R, W, S, F, A] = new ReaderWriterStateT[R, W, S, F, A] {
    val runT = k
  }
}

trait ReaderWriterStateTs {
  type ReaderWriterState[R, W, S, A] =
  ReaderWriterStateT[R, W, S, Identity, A]

  type RWST[R, W, S, F[_], A] =
  ReaderWriterStateT[R, W, S, F, A]

  type RWS[R, W, S, A] =
  ReaderWriterState[R, W, S, A]

  def readerWriterStateT[R, W, S, F[_], A](k: R => S => F[(A, S, W)]): ReaderWriterStateT[R, W, S, F, A] = new ReaderWriterStateT[R, W, S, F, A] {
    val runT = k
  }

  def readerWriterState[R, W, S, A](k: R => S => (A, S, W)): ReaderWriterState[R, W, S, A] = new ReaderWriterState[R, W, S, A] {
    val runT = (r: R) => (s: S) => Identity.id(k(r)(s))
  }
}