// Regression test for the bundled cube.spr tensegrity model:
// six equal, non-touching struts in cubic geometry, 24 cables.

package com.springie.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.GraphicsEnvironment;

import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.io.in.DataInput;

class CubeTensegrityTest {

  private static double linkLength(Link link) {
    final Node a = link.nodes[0];
    final Node b = link.nodes[1];
    final double dx = a.pos.x - b.pos.x;
    final double dy = a.pos.y - b.pos.y;
    final double dz = a.pos.z - b.pos.z;
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  @Test
  void cubeIsAStableTensegrity() {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    final NodeManager manager = new NodeManager();
    ContextManager.setNodeManager(manager);
    new DataInput(manager).loadFile("resource://models/cube.spr");

    assertEquals(12, manager.element.size(), "cube: 12 nodes");
    final LinkManager links = manager.getLinkManager();
    assertEquals(30, links.element.size(), "cube: 6 struts + 24 cables");

    int struts = 0;
    int cables = 0;
    for (int i = 0; i < links.element.size(); i++) {
      final Link link = (Link) links.element.get(i);
      if (!link.type.tension) {
        struts++;
      }
      if (!link.type.compression) {
        cables++;
      }
    }
    assertEquals(6, struts, "cube: 6 compression-only struts");
    assertEquals(24, cables, "cube: 24 tension-only cables");

    // no two struts share a node (they must not touch or join)
    final java.util.Set<Node> strutNodes = new java.util.HashSet<>();
    for (int i = 0; i < links.element.size(); i++) {
      final Link link = (Link) links.element.get(i);
      if (!link.type.tension) {
        assertTrue(strutNodes.add(link.nodes[0]), "strut endpoints are unique");
        assertTrue(strutNodes.add(link.nodes[1]), "strut endpoints are unique");
      }
    }

    // the loader scales models to fit the screen; measure stability
    // relative to the post-load lengths
    final double[] initial = new double[links.element.size()];
    for (int i = 0; i < initial.length; i++) {
      initial[i] = linkLength((Link) links.element.get(i));
    }

    FrEnd.paused = false;
    for (int step = 0; step < 4000; step++) {
      manager.nodeAndLinkUpdate();
    }

    double strutMin = Double.MAX_VALUE;
    double strutMax = 0;
    for (int i = 0; i < links.element.size(); i++) {
      final Link link = (Link) links.element.get(i);
      final double length = linkLength(link);
      final double ratio = length / initial[i];
      assertTrue(ratio > 0.95 && ratio < 1.05,
          "link " + i + " holds its length under dynamics");
      if (!link.type.tension) {
        strutMin = Math.min(strutMin, length);
        strutMax = Math.max(strutMax, length);
      }
    }
    assertTrue((strutMax - strutMin) / strutMin < 0.05,
        "the six struts stay equal in length");
  }
}
