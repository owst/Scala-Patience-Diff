val NGSNexus    = "NGS Nexus"     at "http://ngs.hr/nexus/content/groups/public/"
val NGS3rdParty = "NGS 3rd Party" at "http://ngs.hr/nexus/content/repositories/thirdparty/"


// ### BASIC SETTINGS ###

version := "0.1.0"

name := "Patience-Diff"

organization := "com.github.owst"

initialCommands := "import OwenDiff._"


// ### RESOLVERS & PUBLISHING ###

resolvers := Seq(NGSNexus)

externalResolvers := Resolver.withDefaultResolvers(resolvers.value, mavenCentral = false)

publishTo := Some(NGS3rdParty)

credentials += Credentials(Path.userHome / ".config" / "Scala-Patience-Diff" / "nexus.config")


// ### JAVA & SCALA COMPILER SETTINGS ###

javaHome := sys.env.get("JDK16_HOME").map(file(_))

javacOptions := Seq(
  "-deprecation"
, "-encoding", "UTF-8"
, "-Xlint:unchecked"
, "-source", "1.6"
, "-target", "1.6"
)

crossScalaVersions := Seq(
  "2.8.1", "2.8.2"
, "2.9.0", "2.9.0-1", "2.9.1", "2.9.1-1", "2.9.2", "2.9.3"
, "2.10.3-RC2"
, "2.11.0-M5"
)

scalaVersion := crossScalaVersions.value.find(_ startsWith "2.10").get

scalacOptions := {
  val scala2_8 = Seq(
    "-unchecked"
  , "-deprecation"
  , "-encoding", "UTF-8"
  , "-optimise"
  , "-Xcheckinit"
  , "-Yclosure-elim"
  , "-Ydead-code"
  , "-Yinline"
  )
// --
  val scala2_9 = Seq(
    "-Xmax-classfile-name", "72"
  )
// --
  val scala2_9_1 = Seq(
    "-Yrepl-sync"
  , "-Xlint"
  , "-Xverify"
  , "-Ywarn-all"
  )
// --
  val scala2_10 = Seq(
    "-feature"
  , "-language:postfixOps"
  , "-language:implicitConversions"
  , "-language:existentials"
  )
// --
  scala2_8 ++ (scalaVersion.value match {
    case x if (x startsWith "2.10.")                => scala2_9 ++ scala2_9_1 ++ scala2_10
    case x if (x startsWith "2.9.") && x >= "2.9.1" => scala2_9 ++ scala2_9_1
    case x if (x startsWith "2.9.")                 => scala2_9
    case _ => Nil
  } )
}

unmanagedSourceDirectories in Compile := (scalaSource in Compile).value :: Nil

unmanagedSourceDirectories in Test := (scalaSource in Test).value :: Nil


// ### TEST DEPENDENCIES ###

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" %
    (scalaVersion.value match {
      case x if x startsWith "2.8." => "1.8"
      case x if x startsWith "2.9." => "2.0.M5b"
      case x if x startsWith "2.10." => "2.0.M8"
      case x if x startsWith "2.11." => "2.0.M7"
    } ) % "test"
, "junit" % "junit" % "4.11" % "test"
)


// ### ECLIPSE SETTINGS ###

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

EclipseKeys.executionEnvironment := Some(EclipseExecutionEnvironment.JavaSE16)
