/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mahout.cf

import org.apache.mahout.math.drm._
import org.apache.mahout.math.scalabindings.{MatrixOps, _}
import org.apache.mahout.sparkbindings.test.DistributedSparkSuite
import org.apache.mahout.test.MahoutSuite
import org.scalatest.FunSuite

/* values 
A =
1	1	0	0	0
0	0	1	1	0
0	0	0	0	1
1	0	0	1	0

B =
1	1	1	1	0
1	1	1	1	0
0	0	1	0	1
1	1	0	1	0
 */

class CooccurrenceAnalysisSuite extends FunSuite with MahoutSuite with DistributedSparkSuite {

  // correct cooccurrence with LLR
  final val matrixLLRCoocAtAControl = dense(
    (0.0,                1.7260924347106847, 0.0,                     0.0,                0.0),
    (1.7260924347106847, 0.0,                0.0,                     0.0,                0.0),
    (0.0,                0.0,                0.0,                     1.7260924347106847, 0.0),
    (0.0,                0.0,                1.7260924347106847,      0.0,                0.0),
    (0.0,                0.0,                0.0,                     0.0,                0.0))

  // correct cross-cooccurrence with LLR
  final val matrixLLRCoocBtAControl = dense(
    (1.7260924347106847, 0.6795961471815897, 0.6795961471815897, 1.7260924347106847, 0.0),
    (1.7260924347106847, 0.6795961471815897, 0.6795961471815897, 1.7260924347106847, 0.0),
    (1.7260924347106847, 0.6795961471815897, 0.6795961471815897, 1.7260924347106847, 0.6795961471815897),
    (1.7260924347106847, 0.6795961471815897, 0.6795961471815897, 1.7260924347106847, 0.0),
    (0.0,                0.0,                0.0,                0.0,                4.498681156950466))



  test("cooccurrence [A'A], [B'A] boolbean data using LLR") {
    val a = dense(
        (1, 1, 0, 0, 0),
        (0, 0, 1, 1, 0),
        (0, 0, 0, 0, 1),
        (1, 0, 0, 1, 0))

    val b = dense(
        (1, 1, 1, 1, 0),
        (1, 1, 1, 1, 0),
        (0, 0, 1, 0, 1),
        (1, 1, 0, 1, 0))

    val drmA = drmParallelize(m = a, numPartitions = 2)
    val drmB = drmParallelize(m = b, numPartitions = 2)

    //self similarity
    val drmCooc = CooccurrenceAnalysis.cooccurrences(drmARaw = drmA, randomSeed = 1, drmBs = Array(drmB))
    val matrixSelfCooc = drmCooc(0).checkpoint().collect
    val diffMatrix = matrixSelfCooc.minus(matrixLLRCoocAtAControl)
    var n = (new MatrixOps(m = diffMatrix)).norm
    n should be < 1E-10

    //cross similarity
    val matrixCrossCooc = drmCooc(1).checkpoint().collect
    val diff2Matrix = matrixCrossCooc.minus(matrixLLRCoocBtAControl)
    n = (new MatrixOps(m = diff2Matrix)).norm
    n should be < 1E-10

  }

  test("cooccurrence [A'A], [B'A] double data using LLR") {
    val a = dense(
        (100000.0D, 1.0D,  0.0D,  0.0D,     0.0D),
        (     0.0D, 0.0D, 10.0D,  1.0D,     0.0D),
        (     0.0D, 0.0D,  0.0D,  0.0D,  1000.0D),
        (     1.0D, 0.0D,  0.0D, 10.0D,     0.0D))

    val b = dense(
        (10000.0D, 100.0D,     1000.0D,      1.0D,   0.0D),
        (   10.0D,   1.0D, 10000000.0D,     10.0D,   0.0D),
        (    0.0D,   0.0D,     1000.0D,      0.0D, 100.0D),
        (  100.0D,   1.0D,        0.0D, 100000.0D,   0.0D))

    val drmA = drmParallelize(m = a, numPartitions = 2)
    val drmB = drmParallelize(m = b, numPartitions = 2)

    //self similarity
    val drmCooc = CooccurrenceAnalysis.cooccurrences(drmARaw = drmA, drmBs = Array(drmB))
    val matrixSelfCooc = drmCooc(0).checkpoint().collect
    val diffMatrix = matrixSelfCooc.minus(matrixLLRCoocAtAControl)
    var n = (new MatrixOps(m = diffMatrix)).norm
    n should be < 1E-10

    //cross similarity
    val matrixCrossCooc = drmCooc(1).checkpoint().collect
    val diff2Matrix = matrixCrossCooc.minus(matrixLLRCoocBtAControl)
    n = (new MatrixOps(m = diff2Matrix)).norm
    n should be < 1E-10
  }

  test("cooccurrence [A'A], [B'A] integer data using LLR") {
    val a = dense(
        ( 1000,  10,       0,    0,   0),
        (    0,   0,  -10000,   10,   0),
        (    0,   0,       0,    0, 100),
        (10000,   0,       0, 1000,   0))

    val b = dense(
        (  100, 1000, -10000, 10000,    0),
        (10000, 1000,    100,    10,    0),
        (    0,    0,     10,     0, -100),
        (   10,  100,      0,  1000,    0))

    val drmA = drmParallelize(m = a, numPartitions = 2)
    val drmB = drmParallelize(m = b, numPartitions = 2)

   //self similarity
    val drmCooc = CooccurrenceAnalysis.cooccurrences(drmARaw = drmA, drmBs = Array(drmB))
    //var cp = drmSelfCooc(0).checkpoint()
    //cp.writeDRM("/tmp/cooc-spark/")//to get values written
    val matrixSelfCooc = drmCooc(0).checkpoint().collect
    val diffMatrix = matrixSelfCooc.minus(matrixLLRCoocAtAControl)
    var n = (new MatrixOps(m = diffMatrix)).norm
    n should be < 1E-10

    //cross similarity
    val matrixCrossCooc = drmCooc(1).checkpoint().collect
    val diff2Matrix = matrixCrossCooc.minus(matrixLLRCoocBtAControl)
    n = (new MatrixOps(m = diff2Matrix)).norm
    n should be < 1E-10
  }

  test("LLR calc") {
    val A = dense(
        (1, 1, 0, 0, 0),
        (0, 0, 1, 1, 0),
        (0, 0, 0, 0, 1),
        (1, 0, 0, 1, 0))

    val AtA = A.transpose().times(A)

    /* AtA is:
      0  =>	{0:2.0,1:1.0,3:1.0}
      1  =>	{0:1.0,1:1.0}
      2  =>	{2:1.0,3:1.0}
      3  =>	{0:1.0,2:1.0,3:2.0}
      4  =>	{4:1.0}

          val AtAd = dense(
         (2, 1, 0, 1, 0),
         (1, 1, 0, 0, 0),
         (0, 0, 1, 1, 0),
         (1, 0, 1, 2, 0),
         (0, 0, 0, 0, 1))

         val AtAdNoSelfCooc = dense(
         (0, 1, 0, 1, 0),
         (1, 0, 0, 0, 0),
         (0, 0, 0, 1, 0),
         (1, 0, 1, 0, 0),
         (0, 0, 0, 0, 0))

        for (MatrixSlice row : cooccurrence) {
            for (Vector.Element element : row.vector().nonZeroes()) {
                long k11 = (long) element.get();// = 1
                long k12 = (long) (rowSums.get(row.index()) - k11);// = 0
                long k21 = (long) (colSums.get(element.index()) - k11);// = 1
                long k22 = (long) (total - k11 - k12 - k21);// = 2
                double score = LogLikelihood.rootLogLikelihoodRatio(k11, k12, k21, k22);
                element.set(score);
            }
        }

        for some reason the hadoop version returns the following
        return 1.0 - 1.0 / (1.0 + logLikelihood);
        so not a pure llr or root llr

    */

    //item (1,0)
    val numInteractionsWithAandB = 1L
    val numInteractionsWithA = 1L
    val numInteractionsWithB = 2L
    val numInteractions = 6l

    val llr = CooccurrenceAnalysis.logLikelihoodRatio(numInteractionsWithA, numInteractionsWithB, numInteractionsWithAandB, numInteractions)

    assert(llr == 2.6341457841558764) // value calculated by hadoop itemsimilairty
  }

  test("downsampling by number per row") {
    val a = dense(
        (1, 1, 1, 1, 0),
        (1, 1, 1, 1, 1),
        (0, 0, 0, 0, 1),
        (1, 1, 0, 1, 0))
    val drmA: DrmLike[Int] = drmParallelize(m = a, numPartitions = 2)

    val downSampledDrm = CooccurrenceAnalysis.sampleDownAndBinarize(drmA, 0xdeadbeef, 4)
    //count non-zero values, should be == 7
    var numValues = 0
    val m = downSampledDrm.collect
    val it = m.iterator()
    while (it.hasNext) {
      val v = it.next().vector()
      val nonZeroIt = v.nonZeroes().iterator()
      while (nonZeroIt.hasNext) {
        numValues += 1
        nonZeroIt.next()
      }
    }

    assert(numValues == 8) //Don't change the random seed or this may fail.
  }
}
