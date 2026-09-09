package com.springie.utilities.math;

import java.util.SplittableRandom;
import java.util.function.IntUnaryOperator;

/**
 * Accuracy analysis for the integer square-root candidates.
 *
 * <p>For every candidate this measures, against the true
 * {@code floor(sqrt(x))} and the true {@code round(sqrt(x))}:
 * <ul>
 *   <li>max absolute error, mean absolute error and the fraction of exact
 *   answers, over every perfect square {@code k*k} and its neighbours
 *   {@code k*k - 1}, {@code k*k + 1} for {@code k} in {@code [0, 46341]}
 *   (reference values known analytically, no floating point involved), plus
 *   <li>the same statistics over a reproducible pseudo-random sample of the
 *   full {@code [0, 2^31)} range (reference: {@code StrictMath.sqrt}).
 * </ul>
 *
 * <p>Checksums of all results are printed so the JIT cannot dead-code the
 * computations away.
 *
 * <p>Run with: {@code mvn test-compile}, then
 * {@code java -cp target/test-classes:target/classes
 * com.springie.utilities.math.SquareRootAnalysis}
 */
public final class SquareRootAnalysis {

  private static final int RANDOM_SAMPLES = 1_000_000;

  private record Candidate(String name, IntUnaryOperator fn) {}

  private static final Candidate[] CANDIDATES = {
      new Candidate("sqrt (incumbent)", SquareRoot::sqrt),
      new Candidate("fastSqrt (incumbent)", SquareRoot::fastSqrt),
      new Candidate("accurateSqrt (incumbent)", SquareRoot::accurateSqrt),
      new Candidate("intrinsicSqrt", SqrtCandidates::intrinsicSqrt),
      new Candidate("newtonBitSqrt", SqrtCandidates::newtonBitSqrt),
      new Candidate("digitSqrt", SqrtCandidates::digitSqrt),
      new Candidate("floatSqrt", SqrtCandidates::floatSqrt),
      new Candidate("tableOneNewtonSqrt", SqrtCandidates::tableOneNewtonSqrt),
  };

  public static void main(String[] args) {
    System.out.println("candidate | maxErrFloor | meanErrFloor | exactFloor% "
        + "| maxErrRound | meanErrRound | checksum");
    System.out.println("-".repeat(95));
    for (Candidate c : CANDIDATES) {
      analyze(c);
    }
  }

  private static void analyze(Candidate c) {
    long maxErrFloor = 0;
    double sumErrFloor = 0;
    long exactFloor = 0;
    long maxErrRound = 0;
    double sumErrRound = 0;
    long count = 0;
    long checksum = 0;

    // 1. Analytic points: every k*k and its neighbours, k in [0, 46340].
    //    floor(sqrt(k*k)) = k, floor(sqrt(k*k-1)) = k-1 (k>=1),
    //    floor(sqrt(k*k+1)) = k. No floating point involved.
    //    Integer.MAX_VALUE is added explicitly (46341*46341 overflows int).
    for (int k = 0; k <= 46340; k++) {
      final long k2 = (long) k * k;
      count += check(c, (int) k2, k, k);
      if (k >= 2) {
        // sqrt(k*k-1) is just below k, so it rounds to k.
        count += check(c, (int) (k2 - 1), k - 1, k);
      }
      if (k >= 1) {
        // (for k=0, x=1 has floor 1, not 0)
        count += check(c, (int) (k2 + 1), k, k);
      }
    }
    // sqrt(2^31 - 1) = 46340.95...: floor 46340, rounds to 46341.
    count += check(c, Integer.MAX_VALUE, 46340, 46341);
    maxErrFloor = Math.max(maxErrFloor, Sums.HOLDER.maxErrFloor);
    sumErrFloor += Sums.HOLDER.sumErrFloor;
    exactFloor += Sums.HOLDER.exactFloor;
    maxErrRound = Math.max(maxErrRound, Sums.HOLDER.maxErrRound);
    sumErrRound += Sums.HOLDER.sumErrRound;
    checksum += Sums.HOLDER.checksum;
    Sums.HOLDER.reset();

    // 2. Reproducible random sample of the full range, vs StrictMath.
    final SplittableRandom random = new SplittableRandom(0x5EED);
    for (int i = 0; i < RANDOM_SAMPLES; i++) {
      final int x = random.nextInt(Integer.MAX_VALUE);
      final int floor = (int) StrictMath.sqrt(x);
      final int round = (int) (StrictMath.sqrt(x) + 0.5);
      count++;
      final int got;
      try {
        got = c.fn.applyAsInt(x);
      } catch (RuntimeException e) {
        throw new AssertionError(c.name + " threw on x=" + x, e);
      }
      final long eFloor = Math.abs((long) got - floor);
      final long eRound = Math.abs((long) got - round);
      if (eFloor > maxErrFloor) {
        maxErrFloor = eFloor;
      }
      if (eRound > maxErrRound) {
        maxErrRound = eRound;
      }
      sumErrFloor += eFloor;
      sumErrRound += eRound;
      if (eFloor == 0) {
        exactFloor++;
      }
      checksum = checksum * 31 + got;
    }

    System.out.printf("%-22s | %11d | %12.4f | %10.4f | %11d | %12.4f | %016x%n",
        c.name, maxErrFloor, sumErrFloor / count, 100.0 * exactFloor / count,
        maxErrRound, sumErrRound / count, checksum);
  }

  /** Mutable accumulator so the analytic loop can share code with main. */
  private static final class Sums {
    static final Sums HOLDER = new Sums();
    long maxErrFloor;
    double sumErrFloor;
    long exactFloor;
    long maxErrRound;
    double sumErrRound;
    long checksum;

    void reset() {
      maxErrFloor = 0;
      sumErrFloor = 0;
      exactFloor = 0;
      maxErrRound = 0;
      sumErrRound = 0;
      checksum = 0;
    }
  }

  /**
   * Checks one analytic point against the known-true floor and round values;
   * returns 1 (one value checked).
   */
  private static int check(Candidate c, int x, int trueFloor, int trueRound) {
    final Sums s = Sums.HOLDER;
    final int got = c.fn.applyAsInt(x);
    final long eFloor = Math.abs((long) got - trueFloor);
    if (eFloor > s.maxErrFloor) {
      s.maxErrFloor = eFloor;
    }
    s.sumErrFloor += eFloor;
    if (eFloor == 0) {
      s.exactFloor++;
    }
    final long eRound = Math.abs((long) got - trueRound);
    if (eRound > s.maxErrRound) {
      s.maxErrRound = eRound;
    }
    s.sumErrRound += eRound;
    s.checksum = s.checksum * 31 + got;
    return 1;
  }
}
