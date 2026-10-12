// This code has been placed into the public domain by its author

package com.springie.scene;
import com.springie.geometry.Coords;

import com.springie.elements.DeepObjectColourCalculator;
import com.springie.elements.links.Link;
import com.springie.elements.nodes.Node;
import com.springie.geometry.Point3D;
import com.springie.utilities.math.SquareRoot;
import java.awt.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CachedLink {
  private static final Logger logger = LoggerFactory.getLogger(CachedLink.class);

  /** Pastel link-category colors (Tim, 2026-09-30): muscles are pastel
   * rose, struts (compression members) are pastel blue, cables (tension
   * members) are pastel mint. Different link categories get different
   * colors; the model's base clazz color is no longer used for links. */
  private static final int COLOUR_MUSCLE = 0xFFB3BA; // pastel rose
  private static final int COLOUR_STRUT = 0xAEC6CF;  // pastel blue
  private static final int COLOUR_CABLE = 0xB5EAD7;  // pastel mint
  private static final int COLOUR_HANDLE = 0xFFF3B0; // pastel yellow

  /**
   * Returns the pastel color for a link's category: handle (wheelbarrow
   * handle) -> yellow; muscle (has a controller) -> rose; strut
   * (compression member) -> blue; cable (tension-only, no controller)
   * -> mint.
   */
  private static int getCategoryColour(final Link link) {
    if (link.handle) {
      return COLOUR_HANDLE;
    }
    if (link.controller != null) {
      return COLOUR_MUSCLE;
    }
    if (link.type.compression) {
      return COLOUR_STRUT;
    }
    return COLOUR_CABLE;
  }

  public Point3D[] preserved_node_start; //= new Point3D(0, 0, 0); // needed?

  public Point3D[] preserved_node_end; //= new Point3D(0, 0, 0); // needed?

  public int radius;

  public int selected_colour;

  public int colour;

  public boolean selected;


  public void cache(final Link link, final int mask) {
    this.radius = link.type.radius;
    this.selected = link.type.selected;

    final int total = link.nodes.length;

    // crude depth calculation :-(

    int depth = 0;
    for (int i = 0; i < total; i++) {
      depth += link.nodes[i].pos.z;
    }
    depth /= total;

    this.preserved_node_start = new Point3D[total]; // new!
    this.preserved_node_end = new Point3D[total]; // new!

    //final int depth = (link.node1.pos.z + link.node2.pos.z) >> 1;
    this.colour = DeepObjectColourCalculator.getColourOfDeepObject(
      getCategoryColour(link), depth)
      & mask;

    this.selected_colour = DeepObjectColourCalculator.getColourOfDeepObject(
      0xFF4040, depth)
      & mask;

    for (int i = 0; i < total - 1; i++) {
      this.preserved_node_start[i] = new Point3D(0, 0, 0);
      this.preserved_node_end[i] = new Point3D(0, 0, 0);
      
      final Node node0 = link.nodes[i];
      final Node node1 = link.nodes[i + 1];

      if (Link.link_display_length == Link.SHORT
        && (!node0.type.hidden || node1.type.hidden)) {
        final int d_x = Link.clampDelta((node0.pos.x - node1.pos.x) >> Coords.shift);
        final int d_y = Link.clampDelta((node0.pos.y - node1.pos.y) >> Coords.shift);
        final int d_z = Link.clampDelta((node0.pos.z - node1.pos.z) >> Coords.shift);

        final int actual_length_squared = (d_x * d_x) + (d_y * d_y)
          + (d_z * d_z);
        final int actual_length = SquareRoot
          .fastSqrt(1 + actual_length_squared);

        final int unit_vector_x = (d_x << Coords.shift) / actual_length;
        final int unit_vector_y = (d_y << Coords.shift) / actual_length;
        final int unit_vector_z = (d_z << Coords.shift) / actual_length;

        if (Link.link_display_length == Link.SHORT && !node0.type.hidden) {
          getPreservedShortNodeCoordinates(this.preserved_node_start[i], node0, -unit_vector_x,
            -unit_vector_y, -unit_vector_z);
        } else {
          getPreservedNodeCoordinates(this.preserved_node_start[i], node0);
        }

        if (Link.link_display_length == Link.SHORT && !node1.type.hidden) {
          getPreservedShortNodeCoordinates(this.preserved_node_end[i], node1, unit_vector_x, unit_vector_y,
            unit_vector_z);
        } else {
          getPreservedNodeCoordinates(this.preserved_node_end[i], node1);
        }
      } else {
        getPreservedNodeCoordinates(this.preserved_node_start[i], node0);
        getPreservedNodeCoordinates(this.preserved_node_end[i], node1);
      }
    }
  }

  private void getPreservedShortNodeCoordinates(final Point3D preserved_node, final Node node,
    final int unit_vector_x, final int unit_vector_y, final int unit_vector_z) {
    //final Node node = link.nodes[i];
    //final Point3D preserved_node_s = this.preserved_node_start[i];
    if (preserved_node == null) {
      logger.debug("getPreservedShortNodeCoordinates:preserved_node_s == null");
    }

    final int r2 = node.type.radius >> Coords.shift;

    preserved_node.z = node.pos.z + (unit_vector_z * r2);
    preserved_node.x = Coords.getXCoords(node.pos.x + (unit_vector_x * r2),
      preserved_node.z);
    preserved_node.y = Coords.getYCoords(node.pos.y + (unit_vector_y * r2),
      preserved_node.z);
  }

  private void getPreservedNodeCoordinates(final Point3D preserved_node, final Node node) {
    //final Node node = link.nodes[i];
    //final Point3D preserved_node = this.preserved_node_start[i];

    preserved_node.z = node.pos.z;
    preserved_node.x = Coords.getXCoords(node.pos.x, preserved_node.z);
    preserved_node.y = Coords.getYCoords(node.pos.y, preserved_node.z);
  }


  public Polygon getStrutPolygon(final Point3D point0, final Point3D point1, final int thicknesss) {
    int d_x = point0.y - point1.y; // u. vec. at
    int d_y = point1.x - point0.x;

    final int start_length = 1 + (SquareRoot
      .fastSqrt((d_x * d_x) + (d_y * d_y)));

    d_x = (d_x * thicknesss) / start_length;
    d_y = (d_y * thicknesss) / start_length;
    final int x1 = point0.x + d_x;
    final int x2 = point1.x + d_x;
    final int x3 = point1.x - d_x;
    final int x4 = point0.x - d_x;

    final int y1 = point0.y + d_y;
    final int y2 = point1.y + d_y;
    final int y3 = point1.y - d_y;
    final int y4 = point0.y - d_y;
    final int[] iax = {x1, x2, x3, x4, };
    final int[] iay = {y1, y2, y3, y4, };
    return new java.awt.Polygon(iax, iay, 4);
  }


}
