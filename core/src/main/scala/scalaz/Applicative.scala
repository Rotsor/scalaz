package scalaz

import java.util.Map.Entry

trait Applicative[F[_]] {
  val pointedFunctor: PointedFunctor[F]
  val applic: Applic[F]

  import Applicative._

  def compose[G[_]](ga: Applicative[G]): Applicative[({type λ[α] = F[G[α]]})#λ] =
    applicativePA[({type λ[α] = F[G[α]]})#λ](
      new Pointed[({type λ[α] = F[G[α]]})#λ] {
        def point[A](a: => A) =
          pointedFunctor.point(ga.point(a))
      }
      , new Applic[({type λ[α] = F[G[α]]})#λ] {
        def applic[A, B](f: F[G[A => B]]) =
          liftA2((ff: G[A => B]) => ga.apply(ff))(f)
      }
    )

  def **[G[_] : Applicative]: Applicative[({type λ[α] = (F[α], G[α])})#λ] = {
    implicit val f = pointedFunctor ** implicitly[Applicative[G]].pointedFunctor
    implicit val a = applic ** implicitly[Applicative[G]].applic
    applicative[({type λ[α] = (F[α], G[α])})#λ]
  }

  def functor: Functor[F] = new Functor[F] {
    def fmap[A, B](f: A => B) = pointedFunctor fmap f
  }

  def pointed: Pointed[F] = new Pointed[F] {
    def point[A](a: => A) = pointedFunctor point a
  }

  def applicFunctor: ApplicFunctor[F] = new ApplicFunctor[F] {
    val applic = Applicative.this.applic
    val functor = pointedFunctor.functor
  }

  def fmap[A, B](f: A => B): F[A] => F[B] =
    functor.fmap(f)

  def point[A](a: => A): F[A] =
    pointed.point(a)

  def apply[A, B](f: F[A => B]): F[A] => F[B] =
    applic.applic(f)

  def liftA2[A, B, C](f: A => B => C): F[A] => F[B] => F[C] =
    a => applic.applic(pointedFunctor.fmap(f)(a))

  def rightAnon[A, B]: F[A] => F[B] => F[B] =
    liftA2(_ => b => b)

  def leftAnon[A, B]: F[A] => F[B] => F[A] =
    liftA2(a => _ => a)

  def rightAnonLift[A, B]: F[A] => B => F[B] =
    a => b => rightAnon(a)(point(b))

  def leftAnonLift[A, B]: F[A] => B => F[A] =
    a => b => leftAnon(a)(point(b))

  def deriving[G[_]](implicit n: ^**^[G, F]): Applicative[G] = {
    implicit val p: PointedFunctor[G] = pointedFunctor.deriving[G]
    implicit val a: Applic[G] = applic.deriving[G]
    applicative[G]
  }

}

object Applicative extends Applicatives

trait Applicatives {
  def applicative[F[_]](implicit p: PointedFunctor[F], a: Applic[F]): Applicative[F] = new Applicative[F] {
    val pointedFunctor = p
    val applic = a
  }

  def applicativePA[F[_]](implicit p: Pointed[F], a: Applic[F]): Applicative[F] = new Applicative[F] {
    val pointedFunctor = new PointedFunctor[F] {
      val functor = new Functor[F] {
        def fmap[A, B](f: A => B) =
          a.applic(p.point(f))
      }
      val pointed = p
    }
    val applic = a
  }

  implicit val OptionApplicative: Applicative[Option] =
    applicative[Option]

  implicit val ListApplicative: Applicative[List] =
    applicative[List]

  implicit val StreamApplicative: Applicative[Stream] =
    applicative[Stream]

  implicit def EitherLeftApplicative[X] =
    applicative[({type λ[α] = Either.LeftProjection[α, X]})#λ]

  implicit def EitherRightApplicative[X] =
    applicative[({type λ[α] = Either.RightProjection[X, α]})#λ]

  implicit def EitherApplicative[X] =
    applicative[({type λ[α] = Either[X, α]})#λ]

  implicit def MapEntryApplicative[X: Monoid] = {
    implicit val z = implicitly[Monoid[X]].zero
    implicit val s = implicitly[Monoid[X]].semigroup
    applicative[({type λ[α] = Entry[X, α]})#λ]
  }

  implicit def Tuple1Applicative: Applicative[Tuple1] =
    applicative[Tuple1]

  implicit def Tuple2Applicative[R: Monoid] = {
    implicit val zr = implicitly[Monoid[R]].zero
    implicit val sr = implicitly[Monoid[R]].semigroup
    applicative[({type λ[α] = (R, α)})#λ]
  }

  implicit def Tuple3Applicative[R: Monoid, S: Monoid] = {
    implicit val zr = implicitly[Monoid[R]].zero
    implicit val sr = implicitly[Monoid[R]].semigroup
    implicit val zs = implicitly[Monoid[S]].zero
    implicit val ss = implicitly[Monoid[S]].semigroup
    applicative[({type λ[α] = (R, S, α)})#λ]
  }

  implicit def Tuple4Applicative[R: Monoid, S: Monoid, T: Monoid] = {
    implicit val zr = implicitly[Monoid[R]].zero
    implicit val sr = implicitly[Monoid[R]].semigroup
    implicit val zs = implicitly[Monoid[S]].zero
    implicit val ss = implicitly[Monoid[S]].semigroup
    implicit val zt = implicitly[Monoid[T]].zero
    implicit val st = implicitly[Monoid[T]].semigroup
    applicative[({type λ[α] = (R, S, T, α)})#λ]
  }

  implicit def Tuple5Applicative[R: Monoid, S: Monoid, T: Monoid, U: Monoid] = {
    implicit val zr = implicitly[Monoid[R]].zero
    implicit val sr = implicitly[Monoid[R]].semigroup
    implicit val zs = implicitly[Monoid[S]].zero
    implicit val ss = implicitly[Monoid[S]].semigroup
    implicit val zt = implicitly[Monoid[T]].zero
    implicit val st = implicitly[Monoid[T]].semigroup
    implicit val zu = implicitly[Monoid[U]].zero
    implicit val su = implicitly[Monoid[U]].semigroup
    applicative[({type λ[α] = (R, S, T, U, α)})#λ]
  }

  implicit def Tuple6Applicative[R: Monoid, S: Monoid, T: Monoid, U: Monoid, V: Monoid] = {
    implicit val zr = implicitly[Monoid[R]].zero
    implicit val sr = implicitly[Monoid[R]].semigroup
    implicit val zs = implicitly[Monoid[S]].zero
    implicit val ss = implicitly[Monoid[S]].semigroup
    implicit val zt = implicitly[Monoid[T]].zero
    implicit val st = implicitly[Monoid[T]].semigroup
    implicit val zu = implicitly[Monoid[U]].zero
    implicit val su = implicitly[Monoid[U]].semigroup
    implicit val zv = implicitly[Monoid[V]].zero
    implicit val sv = implicitly[Monoid[V]].semigroup
    applicative[({type λ[α] = (R, S, T, U, V, α)})#λ]
  }

  implicit def Tuple7Applicative[R: Monoid, S: Monoid, T: Monoid, U: Monoid, V: Monoid, W: Monoid] = {
    implicit val zr = implicitly[Monoid[R]].zero
    implicit val sr = implicitly[Monoid[R]].semigroup
    implicit val zs = implicitly[Monoid[S]].zero
    implicit val ss = implicitly[Monoid[S]].semigroup
    implicit val zt = implicitly[Monoid[T]].zero
    implicit val st = implicitly[Monoid[T]].semigroup
    implicit val zu = implicitly[Monoid[U]].zero
    implicit val su = implicitly[Monoid[U]].semigroup
    implicit val zv = implicitly[Monoid[V]].zero
    implicit val sv = implicitly[Monoid[V]].semigroup
    implicit val zw = implicitly[Monoid[W]].zero
    implicit val sw = implicitly[Monoid[W]].semigroup
    applicative[({type λ[α] = (R, S, T, U, V, W, α)})#λ]
  }

  implicit def Function0Applicative: Applicative[Function0] =
    applicative[Function0]

  implicit def Function1Applicative[R]: Applicative[({type λ[α] = (R) => α})#λ] =
    applicative[({type λ[α] = (R) => α})#λ]

  implicit def Function2Applicative[R, S]: Applicative[({type λ[α] = (R, S) => α})#λ] =
    applicative[({type λ[α] = (R, S) => α})#λ]

  implicit def Function3Applicative[R, S, T]: Applicative[({type λ[α] = (R, S, T) => α})#λ] =
    applicative[({type λ[α] = (R, S, T) => α})#λ]

  implicit def Function4Applicative[R, S, T, U]: Applicative[({type λ[α] = (R, S, T, U) => α})#λ] =
    applicative[({type λ[α] = (R, S, T, U) => α})#λ]

  implicit def Function5Applicative[R, S, T, U, V]: Applicative[({type λ[α] = (R, S, T, U, V) => α})#λ] =
    applicative[({type λ[α] = (R, S, T, U, V) => α})#λ]

  implicit def Function6Applicative[R, S, T, U, V, W]: Applicative[({type λ[α] = (R, S, T, U, V, W) => α})#λ] =
    applicative[({type λ[α] = (R, S, T, U, V, W) => α})#λ]

  implicit val IdentityApplicative: Applicative[Identity] = implicitly[Monad[Identity]].applicative

  implicit def CoKleisliApplicative[F[_], R]: Applicative[({type λ[α] = CoKleisli[R, F, α]})#λ] =
    applicative[({type λ[α] = CoKleisli[R, F, α]})#λ]

  implicit def ConstApplicative[A: Monoid] = {
    implicit val z = implicitly[Monoid[A]].zero
    implicit val s = implicitly[Monoid[A]].semigroup
    applicative[({type λ[α] = Const[A, α]})#λ]
  }

  implicit def KleisliApplicative[F[_], R](implicit ap: Applicative[F]): Applicative[({type λ[α] = Kleisli[R, F, α]})#λ] = {
    implicit val a = ap.applic
    implicit val p = ap.pointedFunctor
    implicit val f = ap.functor
    applicative[({type λ[α] = Kleisli[R, F, α]})#λ]
  }

  implicit val NonEmptyListApplicative: Applicative[NonEmptyList] =
    applicative

  implicit def ReaderWriterStateTApplicative[R, W: Monoid, S, F[_] : Monad]: Applicative[({type λ[α] = ReaderWriterStateT[R, W, S, F, α]})#λ] = {
    implicit val sg = implicitly[Monoid[W]].semigroup
    implicit val z = implicitly[Monoid[W]].zero
    implicit val b = implicitly[Monad[F]].bindFunctor
    implicit val a = implicitly[Monad[F]].applic
    implicit val p = implicitly[Monad[F]].pointedFunctor
    applicative[({type λ[α] = ReaderWriterStateT[R, W, S, F, α]})#λ]
  }

  implicit def StateTApplicative[A, F[_]](implicit m: Monad[F]): Applicative[({type λ[α] = StateT[A, F, α]})#λ] = {
    implicit val b = implicitly[Monad[F]].bindFunctor
    implicit val a = m.applic
    implicit val p = m.pointedFunctor
    applicative[({type λ[α] = StateT[A, F, α]})#λ]
  }

  implicit def StepListTApplicative[F[_]](implicit ap: Applicative[F]): Applicative[({type λ[X] = StepListT[F, X]})#λ] = {
    implicit val p = ap.pointedFunctor
    implicit val ftr = p.functor
    implicit val appl: Applic[F] = ap.applic
    applicative[({type λ[X] = StepListT[F, X]})#λ]
  }

  implicit def StepStreamTApplicative[F[_]](implicit ap: Applicative[F]): Applicative[({type λ[X] = StepStreamT[F, X]})#λ] = {
    implicit val p = ap.pointedFunctor
    implicit val ftr = p.functor
    implicit val appl: Applic[F] = ap.applic
    applicative[({type λ[X] = StepStreamT[F, X]})#λ]
  }

  implicit val TreeApplicative: Applicative[Tree] =
    applicative[Tree]

  implicit def FailProjectionApplicative[X]: Applicative[({type λ[α] = FailProjection[α, X]})#λ] =
    applicative[({type λ[α] = FailProjection[α, X]})#λ]

  implicit def ValidationApplicative[X: Semigroup]: Applicative[({type λ[α] = Validation[X, α]})#λ] =
    applicative[({type λ[α] = Validation[X, α]})#λ]

  implicit def WriterTApplicative[A, F[_]](implicit ap: Applicative[F], n: Monoid[A]): Applicative[({type λ[α] = WriterT[A, F, α]})#λ] = {
    implicit val a = ap.applic
    implicit val p = ap.pointedFunctor
    implicit val f = ap.applicFunctor
    implicit val s = n.semigroup
    implicit val z = n.zero
    applicative[({type λ[α] = WriterT[A, F, α]})#λ]
  }

  implicit def OptionTApplicative[F[_] : Applicative]: Applicative[({type λ[α] = OptionT[F, α]})#λ] = {
    implicit val a = implicitly[Applicative[F]].applic
    implicit val p = implicitly[Applicative[F]].pointedFunctor
    implicit val z = implicitly[Applicative[F]].applicFunctor
    applicative[({type λ[α] = OptionT[F, α]})#λ]
  }

  implicit def LazyOptionTApplicative[F[_] : Applicative]: Applicative[({type λ[α] = LazyOptionT[F, α]})#λ] = {
    implicit val a = implicitly[Applicative[F]].applic
    implicit val p = implicitly[Applicative[F]].pointedFunctor
    implicit val z = implicitly[Applicative[F]].applicFunctor
    applicative[({type λ[α] = LazyOptionT[F, α]})#λ]
  }

  implicit def EitherTApplicative[F[_] : Applicative, A]: Applicative[({type λ[α] = EitherT[A, F, α]})#λ] = {
    implicit val a = implicitly[Applicative[F]].applic
    implicit val p = implicitly[Applicative[F]].pointedFunctor
    implicit val z = implicitly[Applicative[F]].applicFunctor
    applicative[({type λ[α] = EitherT[A, F, α]})#λ]
  }

  implicit def LeftEitherTApplicative[F[_] : Applicative, B]: Applicative[({type λ[α] = EitherT.LeftProjectionT[α, F, B]})#λ] = {
    implicit val a = implicitly[Applicative[F]].applic
    implicit val p = implicitly[Applicative[F]].pointedFunctor
    implicit val z = implicitly[Applicative[F]].applicFunctor
    applicative[({type λ[α] = EitherT.LeftProjectionT[α, F, B]})#λ]
  }

  implicit def LazyEitherTApplicative[F[_] : Applicative, A]: Applicative[({type λ[α] = LazyEitherT[A, F, α]})#λ] = {
    implicit val a = implicitly[Applicative[F]].applic
    implicit val p = implicitly[Applicative[F]].pointedFunctor
    implicit val z = implicitly[Applicative[F]].applicFunctor
    applicative[({type λ[α] = LazyEitherT[A, F, α]})#λ]
  }

  implicit def LazyLeftEitherTApplicative[F[_] : Applicative, B]: Applicative[({type λ[α] = LazyEitherT.LazyLeftProjectionT[α, F, B]})#λ] = {
    implicit val a = implicitly[Applicative[F]].applic
    implicit val p = implicitly[Applicative[F]].pointedFunctor
    implicit val z = implicitly[Applicative[F]].applicFunctor
    applicative[({type λ[α] = LazyEitherT.LazyLeftProjectionT[α, F, B]})#λ]
  }

  implicit val LazyOptionApplicative: Applicative[LazyOption] =
    applicative[LazyOption]

  implicit def LazyEitherApplicative[A]: Applicative[({type λ[α] = LazyEither[A, α]})#λ] =
    applicative[({type λ[α] = LazyEither[A, α]})#λ]

  implicit def LazyLeftEitherApplicative[B]: Applicative[({type λ[α] = LazyEither.LazyLeftProjection[α, B]})#λ] =
    applicative[({type λ[α] = LazyEither.LazyLeftProjection[α, B]})#λ]

  import scala.util.control.TailCalls.TailRec

  implicit val TailRecApplicative: Applicative[TailRec] =
    applicative[TailRec]

  import scala.util.continuations.ControlContext

  implicit def ControlContextApplicative[B]: Applicative[({type T[A] = ControlContext[A, B, B]})#T] =
    applicative[({type T[A] = ControlContext[A, B, B]})#T]

}
