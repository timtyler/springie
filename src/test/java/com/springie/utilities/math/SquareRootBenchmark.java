package com.springie.utilities.math;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Throughput microbenchmarks for the integer square-root candidates.
 *
 * <p>Two input distributions, chosen to bracket real usage:
 * <ul>
 *   <li>{@code small}: 1 .. 1_000_000 -- pixel-scale squared distances, the
 *   dominant case: every {@code fastSqrt} call site passes
 *   {@code 1 + dx*dx + dy*dy} with pixel deltas.
 *   <li>{@code full}: uniform over {@code [0, 2^31)}.
 * </ul>
 *
 * <p>Run with: {@code mvn test-compile}, then
 * {@code java -cp target/test-classes:target/classes:&lt;deps&gt;
 * org.openjdk.jmh.Main SquareRootBenchmark}
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(2)
@State(Scope.Thread)
public class SquareRootBenchmark {

  @Param({"small", "full"})
  public String distribution;

  private int[] inputs;
  private int cursor;

  @Setup
  public void setup() {
    final java.util.SplittableRandom random = new java.util.SplittableRandom(0xC0FFEE);
    inputs = new int[4096];
    for (int i = 0; i < inputs.length; i++) {
      inputs[i] = "small".equals(distribution)
          ? 1 + random.nextInt(1_000_000)
          : random.nextInt(Integer.MAX_VALUE);
    }
    cursor = 0;
  }

  private int nextInput() {
    final int x = inputs[cursor];
    cursor = (cursor + 1) & (inputs.length - 1);
    return x;
  }

  // --- incumbents -------------------------------------------------------

  @Benchmark
  public int sqrt() {
    return SquareRoot.sqrt(nextInput());
  }

  @Benchmark
  public int fastSqrt() {
    return SquareRoot.fastSqrt(nextInput());
  }

  @Benchmark
  public int accurateSqrt() {
    return SquareRoot.accurateSqrt(nextInput());
  }

  // --- candidates -------------------------------------------------------

  @Benchmark
  public int intrinsicSqrt() {
    return SqrtCandidates.intrinsicSqrt(nextInput());
  }

  @Benchmark
  public int newtonBitSqrt() {
    return SqrtCandidates.newtonBitSqrt(nextInput());
  }

  @Benchmark
  public int digitSqrt() {
    return SqrtCandidates.digitSqrt(nextInput());
  }

  @Benchmark
  public int floatSqrt() {
    return SqrtCandidates.floatSqrt(nextInput());
  }

  @Benchmark
  public int tableOneNewtonSqrt() {
    return SqrtCandidates.tableOneNewtonSqrt(nextInput());
  }

  @Benchmark
  public int roundIntrinsicSqrt() {
    return SqrtCandidates.roundIntrinsicSqrt(nextInput());
  }
}
