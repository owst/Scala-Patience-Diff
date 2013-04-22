import sbt._
import Keys._

trait Default {
  private object Repositories {
    val NGSNexus    = "NGS Nexus"     at "http://ngs.hr/nexus/content/groups/public/"
    val NGS3rdParty = "NGS 3rd Party" at "http://ngs.hr/nexus/content/repositories/thirdparty/"
  }
//  ---------------------------------------------------------------------------

  private object Resolvers {
    import Repositories._

    lazy val settings = Seq(
      resolvers := Seq(NGSNexus)
    , externalResolvers <<= resolvers map { r =>
        Resolver.withDefaultResolvers(r, mavenCentral = false)
      }
    )
  }

//  ---------------------------------------------------------------------------

  private object Publishing {
    import Repositories._

    val settings = Seq(
      publishTo := Some(NGS3rdParty)
    , credentials += Credentials(Path.userHome / ".config" / "Scala-Patience-Diff" / "nexus.config")
    )
  }

//  ---------------------------------------------------------------------------

  private object ScalaOptions {
    val scala2_8 = Seq(
      "-unchecked"
    , "-deprecation"
    , "-optimise"
    , "-encoding", "UTF-8"
    , "-Xcheckinit"
    , "-Yclosure-elim"
    , "-Ydead-code"
    , "-Yinline"
    )

    val scala2_9 = Seq(
      "-Xmax-classfile-name", "72"
    )

    val scala2_9_1 = Seq(
      "-Yrepl-sync"
    , "-Xlint"
    , "-Xverify"
    , "-Ywarn-all"
    )

    val scala2_10 = Seq(
      "-feature"
    , "-language:postfixOps"
    , "-language:implicitConversions"
    , "-language:existentials"
    )

    def apply(version: String) = scala2_8 ++ (version match {
      case x if (x startsWith "2.10.")                => scala2_9 ++ scala2_9_1 ++ scala2_10
      case x if (x startsWith "2.9.") && x >= "2.9.1" => scala2_9 ++ scala2_9_1
      case x if (x startsWith "2.9.")                 => scala2_9
      case _ => Nil
    } )
  }

//  ---------------------------------------------------------------------------

  import com.typesafe.sbteclipse.plugin.EclipsePlugin.{ settings => eclipseSettings, _ }

  lazy val scalaSettings =
    Defaults.defaultSettings ++
    Resolvers.settings ++
    Publishing.settings ++
    eclipseSettings ++ Seq(
      javaHome := sys.env.get("JDK16_HOME").map(file(_))
    , javacOptions := Seq(
        "-encoding", "UTF-8"
      )

    , crossScalaVersions := Seq("2.8.0", "2.8.1", "2.8.2", "2.9.0", "2.9.0-1", "2.9.1", "2.9.1-1", "2.9.2", "2.9.3", "2.10.1")
    , scalaVersion <<= crossScalaVersions(_.last)
    , scalacOptions <<= scalaVersion map ScalaOptions.apply

    , unmanagedSourceDirectories in Compile <<= (scalaSource in Compile)(_ :: Nil)
    , unmanagedSourceDirectories in Test := Nil

    , EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource
    )
}
