// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.GraphicsEnvironment;

import org.junit.jupiter.api.Test;

import com.springie.elements.links.Link;
import com.springie.elements.links.LinkType;
import com.springie.elements.links.LinkTypeFactory;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.render.Coords;
import com.springie.elements.clazz.Clazz;
import com.springie.geometry.Point3D;

/**
 * The scene builder must turn struts into stretched spheres and cables
 * into plain cylinders, matching the default renderer's convention
 * (fat bulging struts, constant-radius cables).
 */
public class RaySceneCableTest {
  @Test
  public void cablesBecomeCylindersAndStrutsBecomeEllipsoids() {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");

    final NodeManager manager = new NodeManager();
    final Node a = manager.addNewAgent();
    final Node b = manager.addNewAgent();
    final Node c = manager.addNewAgent();
    a.pos = new Point3D(0, 0, 0);
    b.pos = new Point3D(100000, 0, 0);
    c.pos = new Point3D(200000, 0, 0);

    final LinkTypeFactory types = new LinkTypeFactory();
    final Link strut = new Link(a, b,
        types.getNew(100 << Coords.shift, 50), new Clazz(0));
    final LinkType cable_type = types.getNew(100 << Coords.shift, 50);
    cable_type.compression = false;
    final Link cable = new Link(b, c, cable_type, new Clazz(0));

    manager.getLinkManager().element.add(strut);
    manager.getLinkManager().element.add(cable);

    final Primitive[] primitives = RayScene.build(manager);
    int cylinders = 0;
    int ellipsoids = 0;
    for (final Primitive p : primitives) {
      if (p instanceof RTCylinder) {
        cylinders++;
      } else if (p instanceof RTEllipsoid) {
        ellipsoids++;
      }
    }
    assertEquals(1, cylinders, "the cable must become a cylinder");
    assertEquals(1, ellipsoids, "the strut must stay an ellipsoid");
    assertTrue(primitives.length >= 2, "scene must not be empty");
  }
}
