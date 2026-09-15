// This code has been placed into the public domain by its author.

package com.springie.muscles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.GraphicsEnvironment;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.elements.clazz.Clazz;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkTypeFactory;
import com.springie.elements.nodes.Node;
import com.springie.geometry.Point3D;
import com.springie.render.Coords;

/**
 * Each controller is linked to one oscillator by index; every dynamics
 * step it rewrites the link's adjusted rest length from that oscillator's
 * sine wave. Amplitude and period live in the oscillator; phase lives in
 * the link, shifting the wave per-link.
 */
class GlobalOscillatorControllerTest {

  private boolean old_enabled;
  private int old_active;
  private int old_amplitude;
  private int old_period;
  private int old_phase;
  private Oscillator old_slot_1;

  @BeforeEach
  void setUp() {
    // Node's static initialiser reaches FrEnd, which builds the GUI.
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");

    this.old_enabled = Muscles.enabled;
    this.old_active = Muscles.active_oscillator;
    this.old_slot_1 = Muscles.oscillators[1];

    final Oscillator active = Muscles.activeOscillator();
    this.old_amplitude = active.getAmplitude();
    this.old_period = active.getPeriodTicks();
    this.old_phase = active.getPhase();

    Muscles.enabled = true;
    Muscles.active_oscillator = 0;
    active.setAmplitude((int) (0.25 * Muscles.UNITY));
    active.setPeriodTicks(100);
    active.setPhase(0);
  }

  @AfterEach
  void tearDown() {
    Muscles.enabled = this.old_enabled;
    Muscles.active_oscillator = this.old_active;
    Muscles.oscillators[1] = this.old_slot_1;

    final Oscillator active = Muscles.activeOscillator();
    active.setAmplitude(this.old_amplitude);
    active.setPeriodTicks(this.old_period);
    active.setPhase(this.old_phase);
  }

  private static Link makeLink() {
    final Node n1 = new Node();
    n1.pos = new Point3D(0, 0, 0);
    final Node n2 = new Node();
    n2.pos = new Point3D(100 << Coords.shift, 0, 0);
    return new Link(n1, n2, new LinkTypeFactory().getNew(100 << Coords.shift, 50),
        new Clazz(0));
  }

  @Test
  void adjustedLengthFollowsTheOscillatorSineWave() {
    final Link link = makeLink();
    final int rest_length = link.type.length;
    final Controller controller = new GlobalOscillatorController(Muscles.active_oscillator);

    controller.update(link, 0);
    assertEquals(rest_length, link.adjusted_rest_length, 2, "sin(0) == 0");

    controller.update(link, 25);
    assertEquals(rest_length + rest_length / 4, link.adjusted_rest_length, 2,
        "quarter period: full positive amplitude");

    controller.update(link, 50);
    assertEquals(rest_length, link.adjusted_rest_length, 2, "half period: back to rest");

    controller.update(link, 75);
    assertEquals(rest_length - rest_length / 4, link.adjusted_rest_length, 2,
        "three-quarter period: full negative amplitude");
  }

  @Test
  void controllerFollowsItsLinkedOscillatorNotTheActiveOne() {
    final Oscillator second = new Oscillator();
    second.setAmplitude((int) (0.5 * Muscles.UNITY));
    second.setPeriodTicks(100);
    second.setPhase(0);
    Muscles.oscillators[1] = second;

    final Link link = makeLink();
    new GlobalOscillatorController(1).update(link, 25);

    assertEquals(link.type.length + link.type.length / 2, link.adjusted_rest_length, 2,
        "a controller linked to oscillator 1 must follow oscillator 1's amplitude, "
            + "even while oscillator 0 is active");
  }

  @Test
  void oscillatorPhaseStillApplies() {
    Muscles.activeOscillator().setPhase(25);

    final Link link = makeLink();
    new GlobalOscillatorController(Muscles.active_oscillator).update(link, 0);

    assertEquals(link.type.length + link.type.length / 4, link.adjusted_rest_length, 2,
        "a 25-tick phase on the oscillator must shift the wave");
  }

  @Test
  void linkPhaseShiftsTheWave() {
    final Link link = makeLink();
    link.phase = 25;
    new GlobalOscillatorController(Muscles.active_oscillator).update(link, 0);

    assertEquals(link.type.length + link.type.length / 4, link.adjusted_rest_length, 2,
        "a 25-tick phase on the link must shift the wave by a quarter period");
  }

  @Test
  void linkPhaseAddsToOscillatorPhase() {
    Muscles.activeOscillator().setPhase(25);

    final Link link = makeLink();
    link.phase = 25;
    new GlobalOscillatorController(Muscles.active_oscillator).update(link, 0);

    assertEquals(link.type.length, link.adjusted_rest_length, 2,
        "oscillator phase 25 + link phase 25 = half period: back to rest");
  }

  @Test
  void linkPhaseDefaultsToZero() {
    assertEquals(0, makeLink().phase, "a fresh link must have zero phase");
  }

  @Test
  void missingOscillatorLeavesTheLinkAlone() {
    final Link link = makeLink();
    final int before = link.adjusted_rest_length;

    new GlobalOscillatorController(7).update(link, 25);

    assertTrue(link.adjusted_rest_length == before,
        "an empty oscillator slot must not touch the link");
  }
}
