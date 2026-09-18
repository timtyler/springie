package com.springie.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.GraphicsEnvironment;

import org.junit.jupiter.api.Test;

import com.springie.elements.clazz.Clazz;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkTypeFactory;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.elements.nodes.NodeType;
import com.springie.geometry.Point3D;
import com.springie.render.Coords;

/**
 * Mass is functional: a node's response to link forces scales with inverse
 * mass. The reference mass (NodeType.REFERENCE_LOG_MASS) behaves exactly as
 * nodes always did; heavier nodes respond less, lighter nodes more.
 */
class MassDynamicsTest {

  @Test
  void inverseMassIsIdentityAtReferenceMass() {
    assertEquals(1000, NodeType.applyInverseMass(1000, NodeType.REFERENCE_LOG_MASS));
    assertEquals(-1000, NodeType.applyInverseMass(-1000, NodeType.REFERENCE_LOG_MASS));
    assertEquals(0, NodeType.applyInverseMass(0, NodeType.REFERENCE_LOG_MASS));
  }

  @Test
  void inverseMassHalvesAndDoublesPerLogStep() {
    final int ref = NodeType.REFERENCE_LOG_MASS;
    assertEquals(500, NodeType.applyInverseMass(1000, ref + 1));
    assertEquals(250, NodeType.applyInverseMass(1000, ref + 2));
    assertEquals(2000, NodeType.applyInverseMass(1000, ref - 1));
    assertEquals(4000, NodeType.applyInverseMass(1000, ref - 2));
  }

  @Test
  void inverseMassTruncatesTowardZero() {
    // No floor bias: mirrored deltas must scale symmetrically.
    final int ref = NodeType.REFERENCE_LOG_MASS;
    assertEquals(500, NodeType.applyInverseMass(1001, ref + 1));
    assertEquals(-500, NodeType.applyInverseMass(-1001, ref + 1));
  }

  @Test
  void inverseMassSaturatesInsteadOfOverflowing() {
    final int ref = NodeType.REFERENCE_LOG_MASS;
    assertEquals(Integer.MAX_VALUE, NodeType.applyInverseMass(Integer.MAX_VALUE, ref - 1));
    assertEquals(Integer.MIN_VALUE, NodeType.applyInverseMass(Integer.MIN_VALUE, ref - 1));
    assertEquals(0, NodeType.applyInverseMass(1000, ref + 31));
    assertEquals(Integer.MAX_VALUE, NodeType.applyInverseMass(1, ref - 31));
  }

  @Test
  void heavierNodeMovesLessUnderTheSameSpring() {
    // FrEnd's static initialiser builds the GUI, so this test needs a display
    // (it runs under xvfb in headless CI environments).
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");

    final int ref = NodeType.REFERENCE_LOG_MASS;
    final int refPull = springPullVelocity(ref);
    final int heavyPull = springPullVelocity(ref + 2);
    final int lightPull = springPullVelocity(ref - 2);

    assertTrue(refPull < 0, "the compressed spring should push the node, got " + refPull);
    assertTrue(heavyPull > refPull,
        "heavy node should gain less velocity: heavy=" + heavyPull + " ref=" + refPull);
    assertTrue(lightPull < refPull,
        "light node should gain more velocity: light=" + lightPull + " ref=" + refPull);
  }

  /**
   * Two free nodes joined by a compressed link, both at the given log mass.
   * Runs 20 force ticks and returns node A's spring-induced x velocity
   * (its velocity minus the constructor's initial velocity).
   */
  private static int springPullVelocity(int logMass) {
    final NodeManager manager = new NodeManager();
    final World world = manager;

    final Node a = world.addNewAgent();
    final Node b = world.addNewAgent();
    a.type.setMass(logMass);
    b.type.setMass(logMass);
    final int startAX = 0;
    final int startBX = 2000;
    a.pos = new Point3D(startAX, 0, 0);
    b.pos = new Point3D(startBX, 0, 0);

    final LinkTypeFactory linkTypes = new LinkTypeFactory();
    world.getLinkManager().element.add(
        new Link(a, b, linkTypes.getNew(100 << Coords.shift, 50), new Clazz(0)));

    for (int step = 0; step < 20; step++) {
      world.wrappedLinkExerciser(world.getLinkManager());
    }

    // Node's constructor seeds velocity at (3, 4, 5); subtract it to isolate
    // the spring-induced part.
    return a.velocity.x - 3;
  }
}
