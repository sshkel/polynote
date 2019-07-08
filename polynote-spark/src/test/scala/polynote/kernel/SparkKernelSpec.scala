package polynote.kernel

import cats.effect.IO
import fs2.concurrent.Topic
import polynote.config.PolynoteConfig
import polynote.kernel.lang.{KernelSpec, SparkLanguages}
import polynote.kernel.lang.scal.ScalaSparkInterpreter
import polynote.kernel.util.{KernelContext, Publish}
import polynote.messages.{Notebook, ShortList}

trait SparkKernelSpec extends KernelSpec {
  private val interpFactory = ScalaSparkInterpreter.factory()

  override def getKernelContext(updates: Topic[IO, KernelStatusUpdate]): KernelContext = {
    // we use SparkKernel to get a KernelContext with a proper classpath and to properly set up the session
    val sparkKernel = SparkPolyKernel(() => IO.pure(Notebook("foo", ShortList(Nil), None)), Map.empty, Map("scala" -> interpFactory), updates, config = PolynoteConfig())
    sparkKernel.init().unsafeRunSync() // make sure to init the spark session
    sparkKernel.kernelContext
  }

  def assertSparkScalaOutput(code: Seq[String])(assertion: (Map[String, Any], Seq[Result], Seq[(String, String)]) => Unit): Unit = {
    assertOutput({ (kernelContext: KernelContext, updates: Topic[IO, KernelStatusUpdate]) =>
      interpFactory(Nil, kernelContext)
    }, code)(assertion)
  }

  def assertSparkScalaOutput(code: String)(assertion: (Map[String, Any], Seq[Result], Seq[(String, String)]) => Unit): Unit = {
    assertSparkScalaOutput(Seq(code))(assertion)
  }

}