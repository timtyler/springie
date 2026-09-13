// This code has been placed into the public domain by its author.

package com.springie.world;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.GraphicsEnvironment;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkTypeFactory;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.geometry.Point3D;
import com.springie.render.Coords;
import com.springie.muscles.GlobalOscillatorController;
import com.springie.muscles.Muscles;

/**
 * End-to-end: muscled links driven through the real dynamics loop move the
 * model, while an identical unmuscled model does something measurably
 * different.
 */
class MuscleDynamicsTest {

  private boolean old_enabled;
  private boolean old_check_collisions;
  private int old_amplitude;
  private int old_period;

  @BeforeEach
  void setUp() {
    // FrEnd's static initialiser builds the GUI, so this test needs a display
    // (it runs under xvfb in headless CI environments).
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");

    this.old_enabled = Muscles.enabled;
    this.old_check_collisions = FrEnd.check_collisions;
    this.old_amplitude = Muscles.activeOscillator().getAmplitude();
    this.old_period = Muscles.activeOscillator().getPeriodTicks();

    FrEnd.check_collisions = false;
    Muscles.enabled = true;
    Muscles.activeOscillator().setAmplitude((int) (0.4 * Muscles.UNITY));
    Muscles.activeOscillator().setPeriodTicks(40);
  }

  @AfterEach
  void tearDown() {
    Muscles.enabled = this.old_enabled;
    FrEnd.check_collisions = this.old_check_collisions;
    Muscles.activeOscillator().setAmplitude(this.old_amplitude);
    Muscles.activeOscillator().setPeriodTicks(this.old_period);
  }

  /** A horizontal chain of links; muscled when asked. */
  private static NodeManager buildChain(boolean muscled) {
    final NodeManager nodes_world = new NodeManager();
    final World world = nodes_world;

    // confine() bounces nodes off the containing "egg" node, so provide one.
    final Node egg = world.addNewAgent();
    egg.type.setSize(1 << 24);
    world.associated_node = egg;

    final int n = 6;
    final Node[] nodes = new Node[n];
    for (int i = 0; i < n; i++) {
      final Node node = world.addNewAgent();
      node.pos = new Point3D(i * (100 << Coords.shift), 0, 0);
      nodes[i] = node;
    }

    final LinkTypeFactory link_types = new LinkTypeFactory();
    for (int i = 0; i < n - 1; i++) {
      final Link link = new Link(nodes[i], nodes[i + 1],
          link_types.getNew(100 << Coords.shift, 50), new Clazz(0));
      if (muscled) {
        // Every controller follows the active oscillator; the whole chain
        // breathes in unison.
        link.controller = new GlobalOscillatorController(Muscles.active_oscillator);
      }
      world.getLinkManager().element.add(link);
    }
    return nodes_world;
  }

  private static int[] xsOf(World world) {
    final int n = world.element.size();
    final int[] xs = new int[n];
    for (int i = 0; i < n; i++) {
      xs[i] = ((Node) world.element.get(i)).pos.x;
    }
    return xs;
  }

  @Test
  void pulsingLinksDriveTheModel() {
    final World muscled = buildChain(true);
    final World plain = buildChain(false);

    for (int step = 0; step < 200; step++) {
      muscled.privateWorldUnbufferedUpdate();
      plain.privateWorldUnbufferedUpdate();
    }

    final int[] muscled_xs = xsOf(muscled);
    final int[] plain_xs = xsOf(plain);

    boolean diverged = false;
    for (int i = 0; i < muscled_xs.length; i++) {
      if (Math.abs(muscled_xs[i] - plain_xs[i]) > (10 << Coords.shift)) {
        diverged = true;
        break;
      }
    }
    assertTrue(diverged,
        "the muscled chain must end up somewhere the plain chain did not");

    // And the muscled model must actually have gone somewhere.
    assertNotEquals(0, muscled_xs[1],
        "a muscled node should have moved off its starting x of 0... or anywhere");
  }
}
