package scalaz
package scalacheck

import java.math.BigInteger
import org.scalacheck.{Pretty, Gen, Arbitrary}
import java.io._
import collection.mutable.ArraySeq
import java.util.concurrent.Callable

object ScalazArbitrary extends ScalazArbitrarys

/**
 * Instances of {@link scalacheck.Arbitrary} for many types in Scalaz.
 */
trait ScalazArbitrarys {

  import *._, *->*._, Identity._, Alpha._
  import newtypes._
  import wrap._
  import BooleanW._
  import BigIntegerW._
  import BigIntW._
  import ByteW._
  import CharW._
  import ShortW._
  import IntW._
  import LongW._
  import StreamW._
  import OptionW._
  import Arbitrary._
  import Gen._
  import ScalaCheckBinding._

  // todo report and/or work around compilation error: "scalaz is not an enclosing class"
  // implicit def ShowPretty[A: Show](a: A): Pretty = Pretty { _ => a.show }

  private def arb[A: Arbitrary]: Arbitrary[A] = implicitly[Arbitrary[A]]

  implicit def ImmutableArrayArbitrary[A: Arbitrary : ClassManifest] =
    arbArray[A] ∘ (ImmutableArray.fromArray[A](_))

  implicit def IdentArbitrary[A](implicit a: Arbitrary[A]): Arbitrary[Identity[A]] =
    a ∘ ((x: A) => id(x))

  implicit def UnitArbitrary: Arbitrary[Unit] = Arbitrary(value(()))

  implicit def AlphaArbitrary: Arbitrary[Alpha] = Arbitrary(oneOf(alphas.toSeq))

  implicit def BooleanConjunctionArbitrary: Arbitrary[BooleanConjunction] = arb[Boolean] ∘ ((_: Boolean).|∧|)

  implicit def arbBigInt: Arbitrary[BigInt] = arb[Int].<**>(arb[Int])(_ * _)

  implicit def arbBigInteger: Arbitrary[BigInteger] = arb[BigInt] ∘ (_.bigInteger)

  implicit def BigIntegerMultiplicationArbitrary: Arbitrary[BigIntegerMultiplication] = arb[BigInteger] ∘ ((_: BigInteger).∏)

  implicit def BigIntMultiplicationArbitrary: Arbitrary[BigIntMultiplication] = arb[BigInt] ∘ ((_: BigInt).∏)

  implicit def ByteMultiplicationArbitrary: Arbitrary[ByteMultiplication] = arb[Byte] ∘ ((_: Byte).∏)

  implicit def CharMultiplicationArbitrary: Arbitrary[CharMultiplication] = arb[Char] ∘ ((_: Char).∏)

  implicit def ShortMultiplicationArbitrary: Arbitrary[ShortMultiplication] = arb[Short] ∘ ((_: Short).∏)

  implicit def IntMultiplicationArbitrary: Arbitrary[IntMultiplication] = arb[Int] ∘ ((_: Int).∏)

  implicit def LongMultiplicationArbitrary: Arbitrary[LongMultiplication] = arb[Long] ∘ ((_: Long).∏)

  implicit def DigitArbitrary: Arbitrary[Digit] = Arbitrary(oneOf(Digit.digits.toSeq))

  implicit def NonEmptyListArbitrary[A](implicit a: Arbitrary[A]): Arbitrary[NonEmptyList[A]] = arb[A].<**>(arb[List[A]])(NonEmptyList.nel(_, _))

  implicit def OrderingArbitrary: Arbitrary[Ordering] = Arbitrary(oneOf(LT, EQ, GT))

  implicit def TreeArbitrary[A](implicit a: Arbitrary[A]): Arbitrary[Tree[A]] = Arbitrary {
    def tree(n: Int): Gen[Tree[A]] = n match {
      case 0 => arbitrary[A] ∘ (Tree.leaf(_))
      case n => {
        val nextSize = n.abs / 2
        arbitrary[A].<**>(resize(n, containerOf[Stream, Tree[A]](Arbitrary(tree(nextSize)).arbitrary)))(Tree.node(_, _))
      }
    }
    Gen.sized(tree _)
  }

  implicit def TreeLocArbitrary[A](implicit a: Arbitrary[A]): Arbitrary[TreeLoc[A]] = arb[Tree[A]] ∘ ((t: Tree[A]) => t.loc)

  implicit def ValidationArbitrary[A, B](implicit a: Arbitrary[A], b: Arbitrary[B]): Arbitrary[Validation[A, B]] = arb[Either[A, B]] ∘ (Validation.fromEither _)

  implicit def FailProjectionArbitrary[A, B](implicit a: Arbitrary[A], b: Arbitrary[B]): Arbitrary[FailProjection[A, B]] = arb[Validation[A, B]] ∘ (_.fail)

  implicit def ZipStreamArbitrary[A](implicit a: Arbitrary[A]): Arbitrary[ZipStream[A]] = arb[Stream[A]] ∘ ((s: Stream[A]) => s.ʐ)

  implicit def Tuple1Arbitrary[A](implicit a: Arbitrary[A]): Arbitrary[Tuple1[A]] = arb[A] ∘ ((x: A) => Tuple1(x))

  implicit def Function0Arbitrary[A](implicit a: Arbitrary[A]): Arbitrary[() => A] = arb[A] ∘ ((x: A) => () => x)

  implicit def FirstOptionArbitrary[A](implicit a: Arbitrary[A]): Arbitrary[FirstOption[A]] = arb[Option[A]] ∘ ((x: Option[A]) => x.first)

  implicit def LastOptionArbitrary[A](implicit a: Arbitrary[A]): Arbitrary[LastOption[A]] = arb[Option[A]] ∘ ((x: Option[A]) => x.last)

  implicit def EitherLeftProjectionArbitrary[A, B](implicit a: Arbitrary[A], b: Arbitrary[B]): Arbitrary[Either.LeftProjection[A, B]] = arb[Either[A, B]] ∘ ((x: Either[A, B]) => x.left)

  implicit def EitherRightProjectionArbitrary[A, B](implicit a: Arbitrary[A], b: Arbitrary[B]): Arbitrary[Either.RightProjection[A, B]] = arb[Either[A, B]] ∘ ((x: Either[A, B]) => x.right)

  implicit def ArraySeqArbitrary[A](implicit a: Arbitrary[A]): Arbitrary[ArraySeq[A]] = arb[List[A]] ∘ ((x: List[A]) => ArraySeq(x: _*))

  implicit def DequeueArbitrary[A](implicit a: Arbitrary[A]) = Arbitrary {
    Gen.sized(n => listOfN(n, arbitrary[A]) map (xs => xs.foldLeft(Dequeue.empty[A])(_ ::> _)))
  }

  implicit def CallableArbitrary[A](implicit a: Arbitrary[A]): Arbitrary[Callable[A]] = arb[A] ∘ ((x: A) => x.η[Callable])

  import concurrent.Promise

  implicit def PromiseArbitrary[A](implicit a: Arbitrary[A], s: concurrent.Strategy): Arbitrary[Promise[A]] = arb[A] ∘ ((x: A) => Promise.promise(x))

  implicit def ZipperArbitrary[A](implicit a: Arbitrary[A]): Arbitrary[Zipper[A]] = arb[Stream[A]].<***>(arb[A], arb[Stream[A]])(Zipper.zipper[A](_, _, _))

  // workaround bug in Scalacheck 1.8-SNAPSHOT.
  private def arbDouble: Arbitrary[Double] = Arbitrary { Gen.oneOf(posNum[Double], negNum[Double])}
}
