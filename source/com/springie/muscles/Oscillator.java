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
   * the adjusted rest length; amplitude and period live here, phase can
   * also be shifted per-link (see {@link #getScale(long, int)}).
   */
  public int getScale(long tick) {
    return getScale(tick, 0);
  }

  /**
   * As {@link #getScale(long)}, with an additional phase shift in ticks.
   * Links carry their own phase ({@link com.springie.elements.links.Link#phase})
   * so each link can pulse at a different point in the oscillator's cycle.
   */
  public int getScale(long tick, int phase_shift) {
    final double radians = 2.0 * Math.PI * (tick + this.phase + phase_shift) / this.period_ticks;
    return Muscles.UNITY + (int) (this.amplitude * Math.sin(radians));
  }

  final void mutate() {
    final Hortensius32Fast rnd = new Hortensius32Fast();

    this.amplitude += (rnd.nextInt() >> 24) & 255;
    this.phase += (rnd.nextInt() >> 24) & 255;
  }
}
