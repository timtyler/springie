// This code has been placed into the public domain by its author.

package com.springie.muscles;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
  void compressedLinkHasNegativeStrain() {
    final Link link = makeLink(50 << Coords.shift);

    assertTrue(Sensors.isCompressed(link));
    assertFalse(Sensors.isStretched(link));
    assertTrue(Sensors.strain(link) < 0);
  }

  @Test
  void stretchedLinkHasPositiveStrain() {
    final Link link = makeLink(200 << Coords.shift);

    assertTrue(Sensors.isStretched(link));
    assertFalse(Sensors.isCompressed(link));
    assertTrue(Sensors.strain(link) > 0);
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
  void strainUsesTheEffectiveRestLength() {
    final Link link = makeLink(100 << Coords.shift);
    link.rest_length_scale = Muscles.UNITY / 2;

    Muscles.enabled = true;
    try {
      // The link sits at its unscaled rest length, but the controller has
      // halved the effective rest length, so the link reads as stretched.
      assertTrue(Sensors.isStretched(link));
    } finally {
      Muscles.enabled = false;
    }
  }
}
