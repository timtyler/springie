// This code has been placed into the public domain by its author

package com.springie.render;

import java.awt.Polygon;

import com.springie.FrEnd;
import com.springie.elements.DeepObjectColourCalculator;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkRenderType;
import com.springie.elements.nodes.Node;
import com.springie.geometry.Point3D;
import com.springie.utilities.log.Log;
import com.springie.utilities.math.SquareRoot;

public class CachedLink {
  public Point3D[] preserved_node_start; //= new Point3D(0, 0, 0); // needed?

  public Point3D[] preserved_node_end; //= new Point3D(0, 0, 0); // needed?

  int radius;

  int selected_colour;

  int colour;

  boolean selected;

  public void draw(Link link) {
    if (!FrEnd.render_hidden_links && link.type.hidden) {
      return;
    }

    final int thickness = getThicknesss();
    RendererDelegator.setColour(this.colour);
    renderLink(link, thickness);

    RendererDelegator.setColour(this.selected_colour);
    if (this.selected) {
      renderLink(link, thickness * 21 >> 4);
      renderLink(link, thickness * 11 >> 4);
    }
  }

  public void scrub(Link link) {
    if (!FrEnd.render_hidden_links && link.type.hidden) {
      return;
    }

    final int thickness = getThicknesss();

    scrubLink(link, thickness);

    if (this.selected) {
      scrubLink(link, thickness * 21 >> 4);
      scrubLink(link, thickness * 11 >> 4);
    }
  }

  private void renderLink(Link link, int thickness) {
    final int display_type = link.getDisplayType();

    final int total = this.preserved_node_start.length;
    for (int i = 0; i < total - 1; i++) {
      final Point3D point0 = this.preserved_node_start[i];
      final Point3D point1 = this.preserved_node_end[i];
      switch (display_type) {
        case LinkRenderType.SOLID:
          renderSolid(point0, point1, thickness);

          break;

        case LinkRenderType.MULTIPLE:
          renderMultiple(point0, point1, link, thickness);

          break;

        case LinkRenderType.CIRCLE_THIN:
          renderCircleThin(point0, point1);

          break;

        case LinkRenderType.CIRCLE_THICK:
          renderCircleThick(point0, point1);

          break;

        //        switch (FrEnd.quality) {
        //          case Quality.THICK_OUTLINE:
        //          case Quality.SOLID:
        //          case Quality._QUALITY_1A:
        //            renderCircleFillOval(diameter, radius);
        //
        //            break;
        //
        //          case Quality.MULTIPLE:
        //            renderCircleDrawOval(diameter, radius);
        //
        //            break;
        //
        //          case Quality._QUALITY_3:
        //          case Quality._QUALITY_3A:
        //            diameter >>= 1;
        //            radius >>= 1;
        //            BinGrid.graphics_handle.drawRect(
        //              ((point0.x + point1.x) >> 1)
        //                - radius,
        //              ((point0.y + point1.y) >> 1)
        //                - radius, diameter, diameter);
        //            break;
        //
        //          case Quality._QUALITY_4:
        //          case Quality._QUALITY_4A:
        //            diameter >>= 1;
        //            radius >>= 1;
        //            BinGrid.graphics_handle.fillRect(
        //              ((point0.x + point1.x) >> 1)
        //                - radius,
        //              ((point0.y + point1.y) >> 1)
        //                - radius, diameter, diameter);
        //
        //            break;
        //
        //          default:
        //            throw new RuntimeException("");
        //        }

        case LinkRenderType.DOTTED:
          renderDotted(point0, point1);

          break;

        case LinkRenderType.POINT:
          RendererDelegator.graphics_handle.fillRect(((point0.x + point1.x) >> 1) - 1,
            ((point0.y + point1.y) >> 1) - 1, 2, 2);

          break;

        case LinkRenderType.INVISIBLE:
          // do nothing...
          break;

        default:
          throw new RuntimeException("");
      }
    }
  }

  private void scrubLink(Link link, int thickness) {
    if (this.preserved_node_start == null) {
      return;
    }
    
    final int display_type = link.getDisplayType();

    final int total = this.preserved_node_start.length;
    for (int i = 0; i < total - 1; i++) {
      final Point3D point0 = this.preserved_node_start[i];
      final Point3D point1 = this.preserved_node_end[i];
      switch (display_type) {
        case LinkRenderType.SOLID:
          RendererDelegator.colourZero();
          renderSolid(point0, point1, thickness);
          break;

        case LinkRenderType.MULTIPLE:
          RendererDelegator.colourZero();
          renderMultiple(point0, point1, link, thickness);
          break;

        case LinkRenderType.CIRCLE_THIN:
          RendererDelegator.colourZero();
          renderCircleThin(point0, point1);

          break;

        case LinkRenderType.CIRCLE_THICK:
          RendererDelegator.colourZero();
          renderCircleThick(point0, point1);

          break;

        case LinkRenderType.DOTTED:
          for (int temp = 0; temp <= Link.number_of_dots; temp++) {
            RendererDelegator.graphics_handle
              .clearRect(
                (point0.x * temp) + (point1.x * (Link.number_of_dots - temp)) >> Link.log_number_of_dots,
                (point0.y * temp) + (point1.y * (Link.number_of_dots - temp)) >> Link.log_number_of_dots,
                1, 1);
          }

          break;

        case LinkRenderType.POINT:
          RendererDelegator.graphics_handle.clearRect(((point0.x + point1.x) >> 1) - 1,
            ((point0.y + point1.y) >> 1) - 1, 2, 2);

          break;

        case LinkRenderType.INVISIBLE:
          // do nothing...
          break;

        default:
          throw new RuntimeException("Unknown render type");
      }
    }
  }

  private void renderCircleThick(Point3D point0, Point3D point1) {
    //final int diameter = this.radius << 1; //Math.max(this.radius,
    //link.node2.type.radius) << 1;
    //final int diameter = Math.max(link.node1.type.radius,
    //link.node2.type.radius) << 1;
    //final int radius = diameter >> 1;
    renderCircleFillOval(point0, point1);
  }

  private void renderCircleThin(Point3D point0, Point3D point1) {
    //final int diameter = this.radius << 1;
    renderCircleDrawOval(point0, point1); // diameter, this.radius
    //final int diameter = Math.max(link.node1.type.radius,
    //  link.node2.type.radius) << 1;
    //final int radius = diameter >> 1;
    //renderCircleDrawOval(diameter, radius);
  }

  void cache(Link link, int mask) {
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
      link.clazz.colour, depth)
      & mask;

    this.selected_colour = DeepObjectColourCalculator.getColourOfDeepObject(
      0xFF4040, depth)
      & mask;

    for (int i = 0; i < total - 1; i++) {
      this.preserved_node_start[i] = new Point3D(0, 0, 0);
      this.preserved_node_end[i] = new Point3D(0, 0, 0);
      
      final Node node0 = link.nodes[i];
      final Node node1 = link.nodes[i + 1];

      if ((Link.link_display_length == Link.SHORT)
        && (!node0.type.hidden || node1.type.hidden)) {
        final int d_x = (node0.pos.x - node1.pos.x) >> Coords.shift;
        final int d_y = (node0.pos.y - node1.pos.y) >> Coords.shift;
        final int d_z = (node0.pos.z - node1.pos.z) >> Coords.shift;

        final int actual_length_squared = (d_x * d_x) + (d_y * d_y)
          + (d_z * d_z);
        final int actual_length = SquareRoot
          .fastSqrt(1 + actual_length_squared);

        final int unit_vector_x = (d_x << Coords.shift) / actual_length;
        final int unit_vector_y = (d_y << Coords.shift) / actual_length;
        final int unit_vector_z = (d_z << Coords.shift) / actual_length;

        if ((Link.link_display_length == Link.SHORT) && (!node0.type.hidden)) {
          getPreservedShortNodeCoordinates(this.preserved_node_start[i], node0, -unit_vector_x,
            -unit_vector_y, -unit_vector_z);
        } else {
          getPreservedNodeCoordinates(this.preserved_node_start[i], node0);
        }

        if ((Link.link_display_length == Link.SHORT) && (!node1.type.hidden)) {
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

  private void getPreservedShortNodeCoordinates(Point3D preserved_node, Node node,
    final int unit_vector_x, final int unit_vector_y, final int unit_vector_z) {
    //final Node node = link.nodes[i];
    //final Point3D preserved_node_s = this.preserved_node_start[i];
    if (preserved_node == null) {
      Log.log("getPreservedShortNodeCoordinates:preserved_node_s == null");
    }

    final int r2 = node.type.radius >> Coords.shift;

    preserved_node.z = node.pos.z + (unit_vector_z * r2);
    preserved_node.x = Coords.getXCoords(node.pos.x + (unit_vector_x * r2),
      preserved_node.z);
    preserved_node.y = Coords.getYCoords(node.pos.y + (unit_vector_y * r2),
      preserved_node.z);
  }

  private void getPreservedNodeCoordinates(Point3D preserved_node, Node node) {
    //final Node node = link.nodes[i];
    //final Point3D preserved_node = this.preserved_node_start[i];

    preserved_node.z = node.pos.z;
    preserved_node.x = Coords.getXCoords(node.pos.x, preserved_node.z);
    preserved_node.y = Coords.getYCoords(node.pos.y, preserved_node.z);
  }

  void renderSelectedLink(Link link) {
    final int display_type = link.getDisplayType();

    final int total = link.nodes.length;

    // crude depth calculation :-(

    for (int i = 0; i < total; i++) {
      final Point3D point0 = this.preserved_node_start[i];
      final Point3D point1 = this.preserved_node_end[i];

      switch (display_type) {
        case LinkRenderType.CIRCLE_THICK:
        case LinkRenderType.CIRCLE_THIN:
          RendererDelegator.graphics_handle.drawOval(((point0.x + point1.x) >> 1) - 8,
            ((point0.y + point1.y) >> 1) - 8, 16, 16);

          break;

        default: // TODO: fix
          int d_x = point0.y - point1.y;
          int d_y = point1.x - point0.x;

          final int start_dx = 1 + (SquareRoot.fastSqrt((d_x * d_x)
            + (d_y * d_y))); // pixels...

          d_x = (d_x << 3) / start_dx;
          d_y = (d_y << 3) / start_dx;

          RendererDelegator.graphics_handle.drawLine(point0.x + d_x, point0.y + d_y,
            point1.x + d_x, point1.y + d_y);
          RendererDelegator.graphics_handle.drawLine(point0.x - d_x, point0.y - d_y,
            point1.x - d_x, point1.y - d_y);

          d_x >>= 1;
          d_y >>= 1;

          RendererDelegator.graphics_handle.drawLine(point0.x + d_x, point0.y + d_y,
            point1.x + d_x, point1.y + d_y);
          RendererDelegator.graphics_handle.drawLine(point0.x - d_x, point0.y - d_y,
            point1.x - d_x, point1.y - d_y);

          break;
      }
    }
  }

  void drawStrutWide(Point3D point0, Point3D point1, int thicknesss) {
    int d_x = point0.y - point1.y; // u. vec. at ???? TODO - check this!
    int d_y = point1.x - point0.x;

    final int start_length = 1 + (SquareRoot
      .fastSqrt((d_x * d_x) + (d_y * d_y))); // pixels...

    d_x = (d_x * thicknesss) / start_length;
    d_y = (d_y * thicknesss) / start_length;
    final int x1 = point0.x + d_x;
    final int x2 = point1.x + d_x;
    final int x3 = point0.x - d_x;
    final int x4 = point1.x - d_x;

    final int y1 = point0.y + d_y;
    final int y2 = point1.y + d_y;
    final int y3 = point0.y - d_y;
    final int y4 = point1.y - d_y;
    RendererDelegator.graphics_handle.drawLine(x1, y1, x2, y2);
    RendererDelegator.graphics_handle.drawLine(x3, y3, x4, y4);
  }

  void drawStrutSolid(Point3D point0, Point3D point1, int thicknesss) {
    RendererDelegator.graphics_handle.fillPolygon(getStrutPolygon(point0, point1,
      thicknesss));
  }

  public Polygon getStrutPolygon(Point3D point0, Point3D point1, int thicknesss) {
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

  private void renderCircleDrawOval(Point3D point0, Point3D point1) {
    final int diameter = this.radius;
    RendererDelegator.graphics_handle.drawOval(
      ((point0.x + point1.x) >> 1) - this.radius, ((point0.y + point1.y) >> 1)
        - this.radius, diameter, diameter);
  }

  private void renderCircleFillOval(Point3D point0, Point3D point1) {
    final int diameter = this.radius;
    RendererDelegator.graphics_handle.fillOval(
      ((point0.x + point1.x) >> 1) - this.radius, ((point0.y + point1.y) >> 1)
        - this.radius, diameter, diameter);
  }

  private void renderDotted(Point3D point0, Point3D point1) {
    for (int temp = 0; temp <= Link.number_of_dots; temp++) {
      RendererDelegator.graphics_handle
        .fillRect(
          (point0.x * temp) + (point1.x * (Link.number_of_dots - temp)) >> Link.log_number_of_dots,
          (point0.y * temp) + (point1.y * (Link.number_of_dots - temp)) >> Link.log_number_of_dots,
          1, 1);
    }
  }

  private void renderMultiple(Point3D point0, Point3D point1, int n,
    int thickness) {
    if ((n & 1) != 0) {
      renderSingle(point0, point1);
    }

    if (n > 1) {
      renderTwoOrMore(point0, point1, n, thickness);
    }
  }

  private void renderSingle(Point3D point0, Point3D point1) {
    RendererDelegator.graphics_handle.drawLine(point0.x, point0.y, point1.x, point1.y);
  }

  private void renderSolid(Point3D point0, Point3D point1, int thickness) {
    drawStrutSolid(point0, point1, thickness);
  }

  private void renderMultiple(Point3D point0, Point3D point1, Link link,
    int thickness) {
    if (!link.type.compression) {
      renderMultiple(point0, point1, Link.number_of_cable_render_divisions,
        thickness);
    } else {
      renderMultiple(point0, point1, Link.number_of_strut_render_divisions,
        thickness);
    }
  }

  private void renderTwoOrMore(Point3D point0, Point3D point1, int n,
    int thickness) {
    final int n2 = n - 1;
    final int t = thickness / n2;
    for (int i = 1 + (n & 1); i <= n2; i = i + 2) {
      drawStrutWide(point0, point1, t * i);
    }
  }

  public int getThicknesss() {
    return this.radius >> Coords.shift;
  }
}