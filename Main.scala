import scala.io.Source._
import OwenDiff._
object Main {
    def main(args : Array[String]) : Unit = {
        val file1 = fromFile(args(0)).getLines.toList
        val file2 = fromFile(args(1)).getLines.toList

        val diffList = Diff.diff(file1, file2)

        println(diffList.mkString)
    }
}
