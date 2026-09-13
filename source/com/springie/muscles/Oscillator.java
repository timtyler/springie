package com.springie.muscles;

import com.springie.utilities.random.Hortensius32Fast;


public class Oscillator {
  /**
   * Pulse depth, as a fixed-point fraction of the rest length: 0.25
   * shortens and lengthens each driven link by up to 25%.
   */
  public int amplitude;

  /** Oscillator period, in dynamics ticks. */
  public int period_ticks;

  /** Phase offset into the cycle, in ticks. */
  public int phase;

  public int getAmplitude() {
    return this.amplitude;
  }
  public void setAmplitude(int amplitude) {
    this.amplitude = amplitude;
  }
  public int getPhase() {
    return this.phase;
  }
  public void setPhase(int phase) {
    this.phase = phase;
  }
  public int getPeriodTicks() {
    return this.period_ticks;
  }
  public void setPeriodTicks(int period_ticks) {
    this.period_ticks = period_ticks;
  }

  /**
   * Rest-length scale factor at the given dynamics tick, in fixed point:
   * UNITY + amplitude * sin(2 * pi * (tick + phase) / period_ticks).
   * Controllers linked to this oscillator write it onto their links as
   * the adjusted rest length; amplitude and phase live here, not in the
   * links.
   */
  public int getScale(long tick) {
    final double radians = 2.0 * Math.PI * (tick + this.phase) / this.period_ticks;
    return Muscles.UNITY + (int) (this.amplitude * Math.sin(radians));
  }

  final void mutate() {
    final Hortensius32Fast rnd = new Hortensius32Fast();

    this.amplitude += (rnd.nextInt() >> 24) & 255;
    this.phase += (rnd.nextInt() >> 24) & 255;
  }
}
