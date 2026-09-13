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
 * The global oscillator drives each link's rest-length scale through a sine
 * wave; per-link phase offsets shift the wave in time.
 */
class GlobalOscillatorControllerTest {

  private boolean old_enabled;
  private int old_amplitude;
  private int old_period;

  @BeforeEach
  void setUp() {
    // Node's static initialiser reaches FrEnd, which builds the GUI.
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");

    this.old_enabled = Muscles.enabled;
    this.old_amplitude = Muscles.amplitude;
    this.old_period = Muscles.period_ticks;

    Muscles.enabled = true;
    Muscles.amplitude = (int) (0.25 * Muscles.UNITY);
    Muscles.period_ticks = 100;
  }

  @AfterEach
  void tearDown() {
    Muscles.enabled = this.old_enabled;
    Muscles.amplitude = this.old_amplitude;
    Muscles.period_ticks = this.old_period;
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
  void scaleFollowsASineWave() {
    final Link link = makeLink();
    final Controller controller = new GlobalOscillatorController(0);

    controller.update(link, 0);
    assertEquals(Muscles.UNITY, link.rest_length_scale, 2, "sin(0) == 0");

    controller.update(link, 25);
    assertEquals(Muscles.UNITY + Muscles.amplitude, link.rest_length_scale, 2,
        "quarter period: full positive amplitude");

    controller.update(link, 50);
    assertEquals(Muscles.UNITY, link.rest_length_scale, 2, "half period: back to rest");

    controller.update(link, 75);
    assertEquals(Muscles.UNITY - Muscles.amplitude, link.rest_length_scale, 2,
        "three-quarter period: full negative amplitude");
  }

  @Test
  void phaseShiftsTheWaveInTime() {
    final Link at_phase_0 = makeLink();
    final Link at_phase_25 = makeLink();

    new GlobalOscillatorController(0).update(at_phase_0, 25);
    new GlobalOscillatorController(25).update(at_phase_25, 0);

    assertEquals(at_phase_0.rest_length_scale, at_phase_25.rest_length_scale,
        "a phase offset of N ticks equals starting N ticks later");
  }

  @Test
  void differentPhasesDisagree() {
    final Link link_a = makeLink();
    final Link link_b = makeLink();

    new GlobalOscillatorController(0).update(link_a, 25);
    new GlobalOscillatorController(50).update(link_b, 25);

    assertTrue(Math.abs(link_a.rest_length_scale - link_b.rest_length_scale)
        > Muscles.amplitude, "opposite phases must pull in opposite directions");
  }
}
