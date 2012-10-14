/*
 * Copyright (c) 2011, Owen Stephens
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Owen Stephens nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Owen Stephens BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package OwenDiff
import scala.collection.immutable.HashMap

object Diff {
    type IndexPair = (Int, Int)

    // Find the LCS of common, unique lines.
    def uniqueLCS(lines1 : Seq[String], lines2 : Seq[String])
      : Traversable[IndexPair] = {
        type LineToIndex = HashMap[String,Int]
        type LineAndIndex = (String, Int)

        // Insert into, or mark as duplicate, the line in the map.
        def updateLineMap(map : LineToIndex, lineAndIndex : LineAndIndex) = {
            val (line, index) = lineAndIndex
            map + ((line, if (map.contains(line)) -1 else index))
        }

        val lines1Indices =
         (new LineToIndex() /: lines1.view.zipWithIndex)(updateLineMap)

        // Remove any duplicated entries (marked by value of -1)
        val uniques1 = lines1Indices.filter(kv => kv._2 >= 0)

        // Represents the current state of the mapping fold.
        // Tuple is (file1Uniques, line#In2ToLine#In1, file2Uniques)
        type MappingState =
          (LineToIndex,HashMap[Int,Int], LineToIndex)

        def updateUniqueMaps(state : MappingState, lineAndIndex : LineAndIndex) = {
            val (uniques1, line2ToLine1, uniqueIndices2) = state
            val (line, index) = lineAndIndex

            // Only pay attention to common lines.
            if (uniques1.contains(line)) {
                val newTuple = if (uniqueIndices2.contains(line)) {
                    (uniques1 - line,// Ensure we don't match this line again
                    line2ToLine1 - uniqueIndices2(line),// Not unique, so unset.
                    uniqueIndices2)
                } else {
                    (uniques1,
                    line2ToLine1 + ((index,uniques1(line))),
                    uniqueIndices2 + ((line,index)))
                }

                newTuple
            } else {
                state
            }
        }

        // Find indices of all unique line2s and create mapping between files.
        val lineMaps = (uniques1, HashMap.empty[Int, Int], new LineToIndex())
        val (_,line2ToLine1,_) =
          (lineMaps /: lines2.view.zipWithIndex)(updateUniqueMaps)

        // Order the pairs by the line order in file2.
        val indices1OrderedBy2 = line2ToLine1.toList.sortBy(p => p._1)

        // Create an Ordered[IndexPair], so that pairs are ordered small-big by
        // their 2nd element (line # in file1).
        implicit def IndexPairOrdered(thisVal : IndexPair) = new
          Ordered[IndexPair] {
            def compare(thatVal : IndexPair) = thisVal._2 compare thatVal._2
        }

        // Obtain the LCS of the line pairs by finding the LIS
        // of the pairs.
        val lcs = PatienceSort.LIS(indices1OrderedBy2)(IndexPairOrdered)

        // Swap the returned tuples' order, so we return pairs: (line1#, line2#)
        lcs.map(t => (t._2, t._1))
    }

    def recursiveMatch(lines1 : Seq[String], lines2 : Seq[String],
      bounds1 : (Int, Int), bounds2 : (Int, Int)) : List[IndexPair] = {
        // Catch base-case bounds.
        if (bounds1._1 == bounds1._2 || bounds2._1 == bounds2._2) {
            return Nil
        }

        // Obtain a list of line pairs that form the LCS
        val equalLineIndices = uniqueLCS(lines1.slice(bounds1._1, bounds1._2),
          lines2.slice(bounds2._1, bounds2._2))

        def processIndexPair(lastPosAndMatches : ((Int, Int), List[IndexPair]),
          pair : IndexPair) = {
            val offsetPos1 = pair._1 + bounds1._1
            val offsetPos2 = pair._2 + bounds2._1
            val (lastPos1, lastPos2) = lastPosAndMatches._1

            // Check if we're in a sequence of matching lines, if not, recurse.
	    val localResults = if (
	      (lastPos1 + 1 != offsetPos1 || lastPos2 + 1 != offsetPos2) 
	      && (offsetPos1!=0 && offsetPos2!=0))
		recursiveMatch(lines1, lines2, (lastPos1 + 1, offsetPos1),(lastPos2 + 1, offsetPos2))
              else
		List()

            ((offsetPos1, offsetPos2), lastPosAndMatches._2 ++
              localResults :+ (offsetPos1, offsetPos2))
        }

        // Fold up the list of matched line equalLineIndices, recursing between
        // groups of matching lines.
        val initialTuple = ((bounds1._1 - 1, bounds2._1), List[IndexPair]())
        val (lastPos, returnList) =
          (initialTuple /: equalLineIndices)(processIndexPair)

        val extraList = if (returnList.length > 0) {
            // If we matched at all, look for matches between the last match
            // and the end.
            recursiveMatch(lines1, lines2, (lastPos._1 + 1, bounds1._2),
              (lastPos._2 + 1, bounds2._2))
        } else if (lines1(bounds1._1) == lines2(bounds2._1)) {
            // Find matches at the "start". Catches non-unique, yet equal lines.
            // Collect matches until we pass the bounds or lines don't match.
            def findStartMatches(pos1 : Int, pos2 : Int,
              acc : List[IndexPair]) : (Int, Int, List[IndexPair]) = {
                if (pos1 >= bounds1._2 || pos2 >= bounds2._2 ||
                  lines1(pos1) != lines2(pos2)) {
                    return (pos1, pos2, acc)
                }

                return findStartMatches(pos1 + 1, pos2 + 1, acc :+ (pos1, pos2))
            }

            val (pos1, pos2, startList) =
              findStartMatches(bounds1._1, bounds2._1, Nil)

            // Recurse between the last match at the start and the end.
            startList ++ recursiveMatch(lines1, lines2, (pos1, bounds1._2),
              (pos2, bounds2._2))
        } else if (lines1(bounds1._2 - 1) == lines2(bounds2._2 - 1)) {
            // Find matches at the end of the lines. Catches non-unique, yet
            // equal lines.
            def findEndMatches(pos1 : Int, pos2 : Int, acc : List[IndexPair])
              : (Int, Int, List[IndexPair]) = {
                if (pos1 <= bounds1._1 || pos2 <= bounds2._1 ||
                  lines1(pos1 - 1) != lines2(pos2 - 1)) {
                    return (pos1, pos2, acc)
                } else {
                    return findEndMatches(pos1 - 1, pos2 - 1,
                      acc :+ (pos1, pos2))
                }
            }

            val (pos1, pos2, endList) = findEndMatches(bounds1._2 - 1,
              bounds2._2 - 1, Nil)
            // Find any matches between end matches and last position.
            val endGapList = recursiveMatch(lines1, lines2, (lastPos._1 + 1,
              pos1), (lastPos._2, pos2))

            // Add any matches between end matched and last match first
            // to retain correct ordering.
            endGapList ++ endList
        } else {
            Nil
        }

        returnList ++ extraList
    }

    // Turn increasing sequences of matched lines into a single MatchResult
    def coalesceResults(results : Seq[IndexPair]) : List[MatchResult] = {
        def processMatchResult(acc : ((Int, Int, Int), List[MatchResult]),
          res : IndexPair) = {
            val (index1, index2) = res
            val (offset1, offset2, length) = acc._1
            val list = acc._2
            // Don't match at the start.
            val notFirst = offset1 != -1

            if (notFirst && index1 == offset1 + length &&
                index2 == offset2 + length) {
                ((offset1, offset2, length + 1), list)
            } else {
                val nextList = if (notFirst) {
                    new MatchResult(offset1, offset2, length) :: list
                } else {
                    list
                }

                ((index1, index2, 1), nextList)
            }
        }

        // Fold up the list of matchingLines to join adjacent matches.
        val ((offset1, offset2, length), list) =
          (((-1, -1, 0), List[MatchResult]()) /: results)(processMatchResult)

        // Create a match for anything at the end.
        val finalList = if (length > 0) {
            new MatchResult(offset1, offset2, length) :: list
        } else {
            list
        }

        finalList.reverse
    }

    def diff(lines1 : Seq[String], lines2 : Seq[String])
      : Traversable[DiffResult] = {
        val matchLines = recursiveMatch(lines1, lines2,
          (0, lines1.length), (0, lines2.length))
        val matchBlocks = coalesceResults(matchLines) :+
          MatchResult(lines1.length, lines2.length, 0)

        // Calculate the actual differences, using the equal line indices.
        def processMatchBlock(acc : (IndexPair, List[DiffResult]),
          block : MatchResult) = {
            val index1 = block.file1Index
            val index2 = block.file2Index
            val blockLen = block.length
            val (pos1, pos2) = acc._1
            val list = acc._2

            // Update the change list, by calculating which sort of change
            // has happened, based on line positions.
            val modificationList = if (pos1 < index1 && pos2 < index2) {
                new Modify(pos1, pos2, (index1 - pos1, index2 - pos2),
                  lines1.slice(pos1,index1), lines2.slice(pos2,index2)) :: list
            } else if (pos1 < index1) {
                new Delete(pos1, pos2, (index1 - pos1, 0),
                  lines1.slice(pos1, index1)) :: list
            } else if (pos2 < index2) {
                new Insert(pos1, pos2, (0, index2 - pos2),
                  lines2.slice(pos2, index2)) :: list
            } else {
                list
            }

            val newPos@(newPos1, newPos2) =
              (index1 + blockLen, index2 + blockLen)

            val returnList = if (blockLen > 0) {
                new Equal(newPos1, newPos2, (blockLen, blockLen),
                  lines1.slice(newPos1 - blockLen, newPos1)) :: modificationList
            } else {
                modificationList
            }

            (newPos, returnList)
        }

        // Fold up a current line tuple with a list of
        // line changes that describe going from file1 to file2.
        // Reverse, since we cons'd to create the list
        var initialTuple = ((0, 0), List[DiffResult]())
        (initialTuple /: matchBlocks)(processMatchBlock)._2.reverse
    }
}

/*
 * Used to represent a match result between two files.
 * file1[file1Index:file1Index + length] ==
 * file2[file2Index:file2Index + length]
 */
case class MatchResult(file1Index : Int, file2Index : Int, length : Int)
