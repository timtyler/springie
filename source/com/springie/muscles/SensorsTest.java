// This code has been placed into the public domain by its author.

package com.springie.muscles;

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
 * Strain is negative when a link is shorter than its rest length
 * (compressed / slack) and positive when it is longer (stretched).
 */
class SensorsTest {

  private boolean old_enabled;

  @BeforeEach
  void setUp() {
    // Node's static initialiser reaches FrEnd, which builds the GUI.
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");

    this.old_enabled = Muscles.enabled;
    Muscles.enabled = false;
  }

  @AfterEach
  void tearDown() {
    Muscles.enabled = this.old_enabled;
  }

  private static Link makeLink(int distance) {
    final Node n1 = new Node();
    n1.pos = new Point3D(0, 0, 0);
    final Node n2 = new Node();
    n2.pos = new Point3D(distance, 0, 0);
    return new Link(n1, n2, new LinkTypeFactory().getNew(100 << Coords.shift, 50),
        new Clazz(0));
  }

  @Test
  void compressedLinkReportsCompressionMagnitude() {
    final Link link = makeLink(50 << Coords.shift);

    final int compression = Sensors.compression(link);
    final int stretch = Sensors.stretch(link);
    final int strain = Sensors.strain(link);

    assertTrue(compression > 0, "a squashed link should report compression");
    assertTrue(stretch == 0, "a squashed link should report no stretch");
    // Half the rest length: compression should be ~0.5 of rest length.
    assertTrue(compression > (int) (0.4 * Muscles.UNITY)
        && compression < (int) (0.6 * Muscles.UNITY),
        "compression should quantify ~0.5, was " + compression);
    assertTrue(strain < 0);
  }

  @Test
  void stretchedLinkReportsStretchMagnitude() {
    final Link link = makeLink(200 << Coords.shift);

    final int compression = Sensors.compression(link);
    final int stretch = Sensors.stretch(link);
    final int strain = Sensors.strain(link);

    assertTrue(stretch > 0, "a pulled link should report stretch");
    assertTrue(compression == 0, "a pulled link should report no compression");
    // Twice the rest length: stretch should be ~1.0 of rest length.
    assertTrue(stretch > (int) (0.9 * Muscles.UNITY)
        && stretch < (int) (1.1 * Muscles.UNITY),
        "stretch should quantify ~1.0, was " + stretch);
    assertTrue(strain > 0);
  }

  @Test
  void restingLinkHasZeroStrain() {
    final Link link = makeLink(100 << Coords.shift);

    // getActualLength uses fastSqrt(1 + d^2), so "exactly at rest" reads a
    // hair long; strain must still be negligible.
    assertTrue(Math.abs(Sensors.strain(link)) <= (1 << (Coords.shift - 4)),
        "strain at the rest length should be ~zero");
  }

  @Test
  void strainUsesTheAdjustedRestLength() {
    final Link link = makeLink(100 << Coords.shift);
    link.controller = new GlobalOscillatorController(Muscles.active_oscillator);
    link.adjusted_rest_length = link.type.length / 2;

    Muscles.enabled = true;
    try {
      // The link sits at its default rest length, but the controller has
      // halved the adjusted rest length, so the link reads as stretched.
      assertTrue(Sensors.stretch(link) > 0);
      assertTrue(Sensors.compression(link) == 0);
    } finally {
      Muscles.enabled = false;
    }
  }
}
