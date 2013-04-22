import sbt._
import Keys._

object DiffBuild extends Build with Default {
  lazy val diff = Project(
    "patience-diff"
  , file("OwenDiff")
  , settings = scalaSettings ++ Seq(
      version := "0.1.0"
    , name := "Patience-Diff"
    , organization := "com.github.owst"
    , initialCommands := "import OwenDiff._"
    )
  )
}
