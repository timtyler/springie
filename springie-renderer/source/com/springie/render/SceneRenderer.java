// This code has been placed into the public domain by its author

package com.springie.render;

import com.springie.constants.Quality;
import com.springie.elements.faces.Face;
import com.springie.elements.faces.FaceManager;
import com.springie.elements.faces.FaceRenderTypes;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.links.LinkRenderType;
import com.springie.elements.lists.ListOfIntegers;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.geometry.Coords;
import com.springie.geometry.Point3D;
import com.springie.scene.ArrayOfFacesToRender;
import com.springie.scene.ArrayOfLinksToRender;
import com.springie.scene.CachedFace;
import com.springie.scene.CachedLink;
import com.springie.scene.CachedNode;
import com.springie.utilities.math.SquareRoot;
import com.springie.world.World;
import java.awt.Point;

/**
 * Legacy immediate-mode drawing for the original renderer, plus the
 * scene-assembly orchestration moved out of NodeManager (option C).
 *
 * <p>The per-element caches themselves ({@code CachedNode} etc.) live in
 * {@code com.springie.scene} as pure data; everything here is drawing.
 */
public final class SceneRenderer {
  private SceneRenderer() {
  }

  public static void drawNode(final CachedNode c, final Node node) {
    if (!World.render_hidden_nodes && node.type.hidden) {
      return;
    }

    RendererDelegator.setColour(c.colour);

    final int temp_x = c.pos.x - c.radius;
    final int temp_y = c.pos.y - c.radius;
    final int temp_diameter = c.radius << 1;
    final int thickness = Node.thickness;

    switch (World.quality) {
      case Quality.SOLID:
        renderSolidNode(c, temp_x, temp_y, temp_diameter);
        break;

      case Quality.THICK_OUTLINE:
        renderSolidNode(c, temp_x, temp_y, temp_diameter);

        RendererDelegator.setColour(0);
        RendererDelegator.graphics_handle.fillOval(temp_x + thickness,
          temp_y + thickness, temp_diameter - thickness - thickness,
          temp_diameter - thickness - thickness);
        break;

      case Quality._QUALITY_1A:
        renderSolidNode(c, temp_x, temp_y, temp_diameter);
        break;

      case Quality.MULTIPLE:
        renderMultipleNode(c, temp_x, temp_y, temp_diameter);
        break;

      case Quality.QUALITY_TERRIBLE:
        RendererDelegator.graphics_handle.drawRect(c.pos.x - 1 - (c.radius >> 1),
          c.pos.y - (c.radius >> 1) - 1, c.radius, c.radius);
        break;

      case Quality._QUALITY_4A:
      case Quality._QUALITY_4:
        RendererDelegator.graphics_handle.fillRect(c.pos.x - (c.radius >> 1),
          c.pos.y - (c.radius >> 1), c.radius, c.radius);
        break;

      case Quality._QUALITY_5:
        RendererDelegator.graphics_handle.fillRect(c.pos.x - 1, c.pos.y - 1, 2, 2);

        break;

      case Quality._QUALITY_6:
        break;

      default:
        throw new RuntimeException("");
    }

    plotChargeSymbols(c, node, c.colour);

    if (c.selected) {
      RendererDelegator.setColour(c.selected_colour);
      drawSelectedNode(c, temp_x, temp_y, temp_diameter);
      //      BinGrid.graphics_handle.drawOval(temp_x - 8, temp_y - 8,
      //      temp_diameter + 16, temp_diameter + 16);
    }
  }

  private static void renderSolidNode(final CachedNode c, final int temp_x, final int temp_y, final int temp_diameter) {
    RendererDelegator.graphics_handle.fillOval(temp_x, temp_y, temp_diameter,
      temp_diameter);
  }

  // colour set up?
  public static void scrubNode(final CachedNode c, final Node node) {
    if (!World.render_hidden_nodes && node.type.hidden) {
      return;
    }

    final int temp_x = c.pos.x - c.radius;
    final int temp_y = c.pos.y - c.radius;
    final int temp_diameter = c.radius << 1;

    final int temp2 = World.quality;

    RendererDelegator.graphics_handle.setColor(RendererDelegator.color_background);

    switch (temp2) {
      case Quality.THICK_OUTLINE:
      case Quality.SOLID:
        renderSolidNode(c, temp_x, temp_y, temp_diameter);
        break;

      case Quality._QUALITY_1A:
        RendererDelegator.graphics_handle.fillRect(temp_x, temp_y, temp_diameter,
          temp_diameter);
        break;

      case Quality.MULTIPLE:
        renderMultipleNode(c, temp_x, temp_y, temp_diameter);
        break;

      case Quality.QUALITY_TERRIBLE:
          RendererDelegator.graphics_handle.drawRect(c.pos.x - 1 - (c.radius >> 1),
                  c.pos.y - (c.radius >> 1) - 1, c.radius, c.radius);
        break;

      case Quality._QUALITY_4A:
        RendererDelegator.graphics_handle.fillRect(c.pos.x - (c.radius >> 1),
          c.pos.y - (c.radius >> 1), c.radius, c.radius);
        break;

      case Quality._QUALITY_4:
        RendererDelegator.graphics_handle.fillRect(c.pos.x - (c.radius >> 1),
          c.pos.y - (c.radius >> 1), c.radius, c.radius);
        break;

      case Quality._QUALITY_5:
        RendererDelegator.graphics_handle.fillRect(c.pos.x - 1, c.pos.y - 1, 2, 2);
        break;

      case Quality._QUALITY_6:
        break;

      default:
        throw new RuntimeException("");
    }

    scrubChargeSymbols(c, node);

    if (c.selected) {
      RendererDelegator.colourZero();
      drawSelectedNode(c, temp_x, temp_y, temp_diameter);
    }
  }

  private static void drawSelectedNode(final CachedNode c, final int temp_x, final int temp_y, final int temp_diameter) {
    RendererDelegator.graphics_handle.drawOval(temp_x - 6, temp_y - 6,
      temp_diameter + 12, temp_diameter + 12);
  }

  private static void renderMultipleNode(final CachedNode c, final int x, final int y, final int diameter) {
    final int n = Node.number_of_render_divisions - 1;
    if (n < 0) {
      return;
    }

    final int inner = (diameter * 3) >> 3;
    int delta = 1;
    int target = 0;

    if (n != 0) {
      delta = Math.max(inner / n, 1);
      target = delta * n;
    }

    for (int i = 0; i <= target; i += delta) {
      final int i_o2 = i >> 1;
      final int d_mi = diameter - i;
      RendererDelegator.graphics_handle.drawOval(x + i_o2, y + i_o2, d_mi, d_mi);
    }
  }

  private static void plotChargeSymbols(final CachedNode c, final Node node, final int colour) {
    if (node.type.charge != 0) {
      RendererDelegator.setColour(colour);
      renderTheChargeSymbol(c, node);
    }
  }

  private static void scrubChargeSymbols(final CachedNode c, final Node node) {
    if (node.type.charge != 0) {
      RendererDelegator.setColour(0);
      renderTheChargeSymbol(c, node);
    }
  }

  private static void renderTheChargeSymbol(final CachedNode c, final Node node) {
    if (World.render_charges) {
      if (node.type.charge > 0) {
        plotPositiveChargeSign(c);
      } else {
        plotNegativeChargeSign(c);
      }
    }
  }

  private static void plotPositiveChargeSign(final CachedNode c) {
    final int x1 = c.pos.x - (c.radius >> 1);
    final int x2 = c.pos.x - (c.radius >> 3);
    final int x3 = c.pos.x + (c.radius >> 3);
    final int x4 = c.pos.x + (c.radius >> 1);

    final int y1 = c.pos.y - (c.radius >> 1);
    final int y2 = c.pos.y - (c.radius >> 3);
    final int y3 = c.pos.y + (c.radius >> 3);
    final int y4 = c.pos.y + (c.radius >> 1);

    final int[] x = {x3, x3, x4, x4, x3, x3, x2, x2, x1, x1, x2, x2 };
    final int[] y = {y1, y2, y2, y3, y3, y4, y4, y3, y3, y2, y2, y1 };

    RendererDelegator.graphics_handle.drawPolygon(x, y, 12);
  }

  private static void plotNegativeChargeSign(final CachedNode c) {
    final int x1 = c.pos.x - (c.radius >> 1);

    final int y1 = c.pos.y - (c.radius >> 3);

    RendererDelegator.graphics_handle.drawRect(x1, y1, c.radius, c.radius >> 2);
  }

  public static void renderNodes(final CachedNode c, final Node node, final int mask) {
    if (World.xor) {
      drawNode(c, node);
    } else {
      scrubNode(c, node);
    }

    c.cache(node, mask);
    drawNode(c, node);
  }

  public static void renderLinks(final CachedNode c, final ArrayOfLinksToRender renderer_link, final LinkManager link_manager, final Node n, final int mask) {
    final ListOfIntegers list_of_links = n.list_of_links;
    final int number = list_of_links.size();
    final int total_number = link_manager.element.size();

    renderer_link.ensureCapacity(total_number);

    for (int l = number; --l >= 0;) {
      final int num_of_link = list_of_links.retreive(l);
      final Link link = (Link) link_manager.element.get(num_of_link);
      final CachedLink cached_link = renderer_link.array[num_of_link];
      final Node other = link.theOtherEnd(n);
      if (n.pos.z >= other.pos.z) {
        if (World.xor) {
          drawLink(cached_link, link);
        } else {
          scrubLink(cached_link, link);
        }
        cached_link.cache(link, mask);
        drawLink(cached_link, link);
      }
    }
  }

  public static void renderDummyLinks(final CachedNode c, final ArrayOfLinksToRender renderer_link, final LinkManager link_manager, final Node n, final int mask) {
      final ListOfIntegers list_of_links = n.list_of_links;
      final int number = list_of_links.size();
      final int total_number = link_manager.element.size();

      renderer_link.ensureCapacity(total_number);

      for (int l = number; --l >= 0;) {
        final int num_of_link = list_of_links.retreive(l);
        final Link link = (Link) link_manager.element.get(num_of_link);
        final CachedLink cached_link = renderer_link.array[num_of_link];
        final Node other = link.theOtherEnd(n);
        if (n.pos.z >= other.pos.z) {
//          if (World.xor) {
//            drawLink(cached_link, link);
//          } else {
//            scrubLink(cached_link, link);
//          }
          cached_link.cache(link, mask);
          //drawLink(cached_link, link);
        }
      }
    }

  public static void renderDummyPolygons(final CachedNode c, final ArrayOfFacesToRender renderer_face, FaceManager face_manager, final Node n, final int mask) {
      final ListOfIntegers polygons = n.list_of_polygons;

      final int total_number = face_manager.element.size();
      renderer_face.ensureCapacity(total_number);

      for (int l = polygons.size(); --l >= 0;) {
        final int face_number = polygons.retreive(l);
        final Face polygon = (Face) face_manager.element.get(face_number);
        final CachedFace cached_face = renderer_face.array[face_number];
        int cnt = 0;
        final int npolygon = polygon.nodes.size();

        for (int i = npolygon; --i >= 0;) {
          final Node node = (Node) polygon.nodes.get(i);
          if (n.pos.z >= node.pos.z) {
            cnt++;
          }
        }

        if (cnt == npolygon) {
//          if (World.xor) {
//            drawFace(cached_face, polygon);
//          } else {
//            scrubFace(cached_face, polygon);
//          }
          cached_face.cache(polygon, mask);
//          drawFace(cached_face, polygon);
        }
      }
    }

  public static void renderPolygons(final CachedNode c, final ArrayOfFacesToRender renderer_face, FaceManager face_manager, final Node n, final int mask) {
    final ListOfIntegers polygons = n.list_of_polygons;

    final int total_number = face_manager.element.size();
    renderer_face.ensureCapacity(total_number);

    for (int l = polygons.size(); --l >= 0;) {
      final int face_number = polygons.retreive(l);
      final Face polygon = (Face) face_manager.element.get(face_number);
      final CachedFace cached_face = renderer_face.array[face_number];
      int cnt = 0;
      final int npolygon = polygon.nodes.size();

      for (int i = npolygon; --i >= 0;) {
        final Node node = (Node) polygon.nodes.get(i);
        if (n.pos.z >= node.pos.z) {
          cnt++;
        }
      }

      if (cnt == npolygon) {
        if (World.xor) {
          drawFace(cached_face, polygon);
        } else {
          scrubFace(cached_face, polygon);
        }
        cached_face.cache(polygon, mask);
        drawFace(cached_face, polygon);
      }
    }
  }

  public static void drawLink(final CachedLink cl, final Link link) {
    if (!World.render_hidden_links && link.type.hidden) {
      return;
    }

    final int thickness = getLinkThickness(cl);
    RendererDelegator.setColour(cl.colour);
    renderLink(cl, link, thickness);

    RendererDelegator.setColour(cl.selected_colour);
    if (cl.selected) {
      renderLink(cl, link, thickness * 21 >> 4);
      renderLink(cl, link, thickness * 11 >> 4);
    }
  }

  public static void scrubLink(final CachedLink cl, final Link link) {
    if (!World.render_hidden_links && link.type.hidden) {
      return;
    }

    final int thickness = getLinkThickness(cl);

    scrubLink(cl, link, thickness);

    if (cl.selected) {
      scrubLink(cl, link, thickness * 21 >> 4);
      scrubLink(cl, link, thickness * 11 >> 4);
    }
  }

  private static void renderLink(final CachedLink cl, final Link link, final int thickness) {
    final int display_type = link.getDisplayType();

    final int total = cl.preserved_node_start.length;
    for (int i = 0; i < total - 1; i++) {
      final Point3D point0 = cl.preserved_node_start[i];
      final Point3D point1 = cl.preserved_node_end[i];
      switch (display_type) {
        case LinkRenderType.SOLID:
          renderSolidLink(cl, point0, point1, thickness);

          break;

        case LinkRenderType.MULTIPLE:
          renderMultipleLink(cl, point0, point1, link, thickness);

          break;

        case LinkRenderType.CIRCLE_THIN:
          renderCircleThinLink(cl, point0, point1);

          break;

        case LinkRenderType.CIRCLE_THICK:
          renderCircleThickLink(cl, point0, point1);

          break;

        //        switch (World.quality) {
        //          case Quality.THICK_OUTLINE:
        //          case Quality.SOLID:
        //          case Quality._QUALITY_1A:
        //            renderCircleFillOvalLink(cl, diameter, radius);
        //
        //            break;
        //
        //          case Quality.MULTIPLE:
        //            renderCircleDrawOvalLink(cl, diameter, radius);
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
          renderDottedLink(cl, point0, point1);

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

  private static void scrubLink(final CachedLink cl, final Link link, final int thickness) {
    if (cl.preserved_node_start == null) {
      return;
    }
    
    final int display_type = link.getDisplayType();

    final int total = cl.preserved_node_start.length;
    for (int i = 0; i < total - 1; i++) {
      final Point3D point0 = cl.preserved_node_start[i];
      final Point3D point1 = cl.preserved_node_end[i];
      switch (display_type) {
        case LinkRenderType.SOLID:
          RendererDelegator.colourZero();
          renderSolidLink(cl, point0, point1, thickness);
          break;

        case LinkRenderType.MULTIPLE:
          RendererDelegator.colourZero();
          renderMultipleLink(cl, point0, point1, link, thickness);
          break;

        case LinkRenderType.CIRCLE_THIN:
          RendererDelegator.colourZero();
          renderCircleThinLink(cl, point0, point1);

          break;

        case LinkRenderType.CIRCLE_THICK:
          RendererDelegator.colourZero();
          renderCircleThickLink(cl, point0, point1);

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

  private static void renderCircleThickLink(final CachedLink cl, final Point3D point0, final Point3D point1) {
    //final int diameter = cl.radius << 1; //Math.max(cl.radius,
    //link.node2.type.radius) << 1;
    //final int diameter = Math.max(link.node1.type.radius,
    //link.node2.type.radius) << 1;
    //final int radius = diameter >> 1;
    renderCircleFillOvalLink(cl, point0, point1);
  }

  private static void renderCircleThinLink(final CachedLink cl, final Point3D point0, final Point3D point1) {
    //final int diameter = cl.radius << 1;
    renderCircleDrawOvalLink(cl, point0, point1); // diameter, cl.radius
    //final int diameter = Math.max(link.node1.type.radius,
    //  link.node2.type.radius) << 1;
    //final int radius = diameter >> 1;
    //renderCircleDrawOvalLink(cl, diameter, radius);
  }

  static void renderSelectedLink(final CachedLink cl, final Link link) {
    final int display_type = link.getDisplayType();

    final int total = link.nodes.length;

    // crude depth calculation :-(

    for (int i = 0; i < total; i++) {
      final Point3D point0 = cl.preserved_node_start[i];
      final Point3D point1 = cl.preserved_node_end[i];

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

  static void drawStrutWideLink(final CachedLink cl, final Point3D point0, final Point3D point1, final int thicknesss) {
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

  static void drawStrutSolidLink(final CachedLink cl, final Point3D point0, final Point3D point1, final int thicknesss) {
    RendererDelegator.graphics_handle.fillPolygon(cl.getStrutPolygon(point0, point1,
      thicknesss));
  }

  private static void renderCircleDrawOvalLink(final CachedLink cl, final Point3D point0, final Point3D point1) {
    final int diameter = cl.radius;
    RendererDelegator.graphics_handle.drawOval(
      ((point0.x + point1.x) >> 1) - cl.radius, ((point0.y + point1.y) >> 1)
        - cl.radius, diameter, diameter);
  }

  private static void renderCircleFillOvalLink(final CachedLink cl, final Point3D point0, final Point3D point1) {
    final int diameter = cl.radius;
    RendererDelegator.graphics_handle.fillOval(
      ((point0.x + point1.x) >> 1) - cl.radius, ((point0.y + point1.y) >> 1)
        - cl.radius, diameter, diameter);
  }

  private static void renderDottedLink(final CachedLink cl, final Point3D point0, final Point3D point1) {
    for (int temp = 0; temp <= Link.number_of_dots; temp++) {
      RendererDelegator.graphics_handle
        .fillRect(
          (point0.x * temp) + (point1.x * (Link.number_of_dots - temp)) >> Link.log_number_of_dots,
          (point0.y * temp) + (point1.y * (Link.number_of_dots - temp)) >> Link.log_number_of_dots,
          1, 1);
    }
  }

  private static void renderMultipleLink(final CachedLink cl, final Point3D point0, final Point3D point1, final int n, int thickness) {
    if ((n & 1) != 0) {
      renderSingleLink(cl, point0, point1);
    }

    if (n > 1) {
      renderTwoOrMoreLink(cl, point0, point1, n, thickness);
    }
  }

  private static void renderSingleLink(final CachedLink cl, final Point3D point0, final Point3D point1) {
    RendererDelegator.graphics_handle.drawLine(point0.x, point0.y, point1.x, point1.y);
  }

  private static void renderSolidLink(final CachedLink cl, final Point3D point0, final Point3D point1, final int thickness) {
    drawStrutSolidLink(cl, point0, point1, thickness);
  }

  private static void renderMultipleLink(final CachedLink cl, final Point3D point0, final Point3D point1, final Link link, int thickness) {
    if (!link.type.compression) {
      renderMultipleLink(cl, point0, point1, Link.number_of_cable_render_divisions,
        thickness);
    } else {
      renderMultipleLink(cl, point0, point1, Link.number_of_strut_render_divisions,
        thickness);
    }
  }

  private static void renderTwoOrMoreLink(final CachedLink cl, final Point3D point0, final Point3D point1, final int n, int thickness) {
    final int n2 = n - 1;
    final int t = thickness / n2;
    for (int i = 1 + (n & 1); i <= n2; i = i + 2) {
      drawStrutWideLink(cl, point0, point1, t * i);
    }
  }

  public static int getLinkThickness(final CachedLink cl) {
    return cl.radius >> Coords.shift;
  }

  public static void drawFace(final CachedFace cf, final Face face) {
    if (!face.type.hidden || World.render_hidden_faces) {
      //final Node n1 = (Node) face.node.get(0);

      RendererDelegator.setColour(cf.colour);

      renderFace(cf, face, Face.face_display_type);

      if (cf.selected) {
        RendererDelegator.setColour(cf.selected_colour);
        renderSelectedFace(cf, face);
      }
    }
  }

  public static void scrubFace(final CachedFace cf, final Face face) {
    if (!World.render_hidden_links && face.type.hidden) {
      return;
    }

    RendererDelegator.colourZero();
    renderFace(cf, face, Face.face_display_type);

    if (cf.selected) {
      //BinGrid.colourZero();
      renderSelectedFace(cf, face);
    }
  }

  private static void renderFace(final CachedFace cf, final Face face, final int type) {
    // "Face lines = 0" fills the face in a solid colour instead of
    // drawing the concentric/segmented line pattern.
    if (Face.number_of_render_divisions == 0) {
      renderSolidFace(cf, face);
      return;
    }

    switch (type) {
      case FaceRenderTypes.CONCENTRIC:
        renderThinConcentricFace(cf, face, Face.number_of_render_divisions);

        break;

      case FaceRenderTypes.SEGMENTED:
        renderThinSegmentedFace(cf, face, Face.number_of_render_divisions);
        break;

      case FaceRenderTypes.CLOCKWISE:
        renderThinClockwiseFace(cf, face, Face.number_of_render_divisions);
        break;

      case FaceRenderTypes.ANTI_CLOCKWISE:
        renderThinAntiClockwiseFace(cf, face, Face.number_of_render_divisions);
        break;

      default:
        throw new RuntimeException("");
    }
  }

  /**
   * Fills the face outline in a solid colour. Used when "Face lines" is 0.
   * The points in {@link #render} are already projected screen coordinates,
   * refreshed by {@link #cache(Face, int)}.
   */
  private static void renderSolidFace(final CachedFace cf, final Face face) {
    final int npoints = face.nodes.size();
    if (cf.render.size() != npoints) {
      cf.setUpRenderingScratchSpace(face);
    }

    final int[] xs = new int[npoints];
    final int[] ys = new int[npoints];
    for (int k = npoints; --k >= 0;) {
      final Point p = (Point) cf.render.get(k);
      xs[k] = p.x;
      ys[k] = p.y;
    }

    RendererDelegator.graphics_handle.fillPolygon(xs, ys, npoints);
  }

  private static void renderThinConcentricFace(final CachedFace cf, final Face face, final int num) {
    final int npoints = face.nodes.size();
    final int rpoints = cf.render.size();
    if (rpoints != npoints) {
      cf.setUpRenderingScratchSpace(face);
    }

    final int actual_number = num << 1;

    for (int i = 1; i < actual_number; i = i + 2) {
      final int j = actual_number - i;
      for (int k = npoints; --k >= 0;) {
        final Point r1 = (Point) cf.render.get(k);
        final Point r2 = (Point) cf.render.get((k + 1) % npoints);

        final int x1 = (cf.render_centre.x * i + r1.x * j) / actual_number;
        final int y1 = (cf.render_centre.y * i + r1.y * j) / actual_number;

        final int x2 = (cf.render_centre.x * i + r2.x * j) / actual_number;
        final int y2 = (cf.render_centre.y * i + r2.y * j) / actual_number;

        RendererDelegator.graphics_handle.drawLine(x1, y1, x2, y2);
      }
    }
  }

  private static void renderThinSegmentedFace(final CachedFace cf, final Face face, final int num) {
    final int npoints = face.nodes.size();
    final int rpoints = cf.render.size();
    if (rpoints != npoints) {
      cf.setUpRenderingScratchSpace(face);
    }

    final int actual_number = num << 1;

    for (int k = npoints; --k >= 0;) {
      final Point r1 = (Point) cf.render.get(k);
      final Point r2 = (Point) cf.render.get((k + 1) % npoints);
      final Point r3 = cf.render_centre;
      final int c_x = (r1.x + r2.x) >> 1;
      final int c_y = (r1.y + r2.y) >> 1;

      for (int i = 1; i < actual_number; i = i + 2) {
        final int j = actual_number - i;

        final int x1 = (c_x * i + r1.x * j) / actual_number;
        final int y1 = (c_y * i + r1.y * j) / actual_number;

        final int x2 = (c_x * i + r2.x * j) / actual_number;
        final int y2 = (c_y * i + r2.y * j) / actual_number;

        final int x3 = (c_x * i + r3.x * j) / actual_number;
        final int y3 = (c_y * i + r3.y * j) / actual_number;

        //BinGrid.graphics_handle.drawLine(x1, y1, x2, y2);
        RendererDelegator.graphics_handle.drawLine(x2, y2, x3, y3);
        RendererDelegator.graphics_handle.drawLine(x3, y3, x1, y1);
      }
    }
  }

  private static void renderThinClockwiseFace(final CachedFace cf, final Face face, final int num) {
    final int npoints = face.nodes.size();
    final int rpoints = cf.render.size();
    if (rpoints != npoints) {
      cf.setUpRenderingScratchSpace(face);
    }

    final int actual_number = num << 1;

    for (int i = 1; i < actual_number; i = i + 2) {
      final int j = actual_number - i;
      for (int k = npoints; --k >= 0;) {
        final Point r1 = (Point) cf.render.get(k);
        final Point r2 = (Point) cf.render.get((k + 1) % npoints);

        final int x1 = (r2.x * i + cf.render_centre.x * j) / actual_number;
        final int y1 = (r2.y * i + cf.render_centre.y * j) / actual_number;

        final int x2 = (r2.x * i + r1.x * j) / actual_number;
        final int y2 = (r2.y * i + r1.y * j) / actual_number;

        RendererDelegator.graphics_handle.drawLine(x1, y1, x2, y2);
      }
    }
  }

  private static void renderThinAntiClockwiseFace(final CachedFace cf, final Face face, final int num) {
    final int npoints = face.nodes.size();
    final int rpoints = cf.render.size();
    if (rpoints != npoints) {
      cf.setUpRenderingScratchSpace(face);
    }

    final int actual_number = num << 1;

    for (int i = 1; i < actual_number; i = i + 2) {
      final int j = actual_number - i;
      for (int k = npoints; --k >= 0;) {
        final Point r1 = (Point) cf.render.get(k);
        final Point r2 = (Point) cf.render.get((k + 1) % npoints);

        final int x1 = (r1.x * i + cf.render_centre.x * j) / actual_number;
        final int y1 = (r1.y * i + cf.render_centre.y * j) / actual_number;

        final int x2 = (r1.x * i + r2.x * j) / actual_number;
        final int y2 = (r1.y * i + r2.y * j) / actual_number;

        RendererDelegator.graphics_handle.drawLine(x1, y1, x2, y2);
      }
    }
  }

  static void renderSelectedFace(final CachedFace cf, final Face face) {
    renderFace(cf, face, Face.face_display_type ^ 3);
  }

  // Scene-assembly orchestration, moved from NodeManager (option C).
  // Populates the scene caches; drawing happens in the static helpers above.

  public static void nodeAndLinkRenderDummy(final NodeManager manager) {
    manager.sortIndex();
    final int number_of_nodes = manager.element.size();
    manager.renderer.renderer_node.ensureCapacity(number_of_nodes);
    final int mask = 0xFFFFFFFF;

    for (int counter = number_of_nodes; --counter >= 0;) {
      final int num = manager.node_depth_index[counter];
      final Node node = (Node) manager.element.get(num);
      final CachedNode cached_node = manager.renderer.renderer_node.array[num];

      renderDummyLinks(cached_node, manager.renderer.renderer_link,
        manager.getLinkManager(), node, mask);
      renderDummyPolygons(cached_node, manager.renderer.renderer_face,
        manager.getFaceManager(), node, mask);
    }
  }

  public static void nodeAndLinkRenderNormal(final NodeManager manager) {
    final int number_of_nodes = manager.element.size();
    manager.renderer.renderer_node.ensureCapacity(number_of_nodes);
    final int mask = 0xFFFFFFFF;

    for (int counter = number_of_nodes; --counter >= 0;) {
      final int num = manager.node_depth_index[counter];
      final Node node = (Node) manager.element.get(num);
      final CachedNode cached_node = manager.renderer.renderer_node.array[num];
      if (World.render_nodes) {
        renderNodes(cached_node, node, mask);
      }

      if (World.render_links) {
        renderLinks(cached_node, manager.renderer.renderer_link,
          manager.getLinkManager(), node, mask);
      }

      if (World.render_faces) {
        renderPolygons(cached_node, manager.renderer.renderer_face,
          manager.getFaceManager(), node, mask);
      }
    }
  }

  public static void nodeAndLinkRenderAnaglyph(final NodeManager manager) {
    final int anaglyph_distance = NodeManager.eye_distance;
    final int number_of_nodes = manager.element.size();

    manager.renderer.renderer_node.ensureCapacity(number_of_nodes);
    manager.renderer2.renderer_node.ensureCapacity(number_of_nodes);

    for (int counter = number_of_nodes; --counter >= 0;) {
      final int num = manager.node_depth_index[counter];
      final Node n = (Node) manager.element.get(num);
      final CachedNode cn1 = manager.renderer.renderer_node.array[num];
      final CachedNode cn2 = manager.renderer2.renderer_node.array[num];
      if (World.render_nodes) {
        Coords.shift_constant_x -= anaglyph_distance;
        renderNodes(cn1, n, NodeManager.mask_right);
        Coords.shift_constant_x += anaglyph_distance << 1;
        renderNodes(cn2, n, NodeManager.mask_left);
        Coords.shift_constant_x -= anaglyph_distance;
      }

      if (World.render_links) {
        Coords.shift_constant_x -= anaglyph_distance;
        renderLinks(cn1, manager.renderer.renderer_link,
          manager.getLinkManager(), n, NodeManager.mask_right);
        Coords.shift_constant_x += anaglyph_distance << 1;
        renderLinks(cn2, manager.renderer2.renderer_link,
          manager.getLinkManager(), n, NodeManager.mask_left);
        Coords.shift_constant_x -= anaglyph_distance;
      }

      if (World.render_faces) {
        Coords.shift_constant_x -= anaglyph_distance;
        renderPolygons(cn1, manager.renderer.renderer_face,
          manager.getFaceManager(), n, NodeManager.mask_right);
        Coords.shift_constant_x += anaglyph_distance << 1;
        renderPolygons(cn1, manager.renderer2.renderer_face,
          manager.getFaceManager(), n, NodeManager.mask_left);
        Coords.shift_constant_x -= anaglyph_distance;
      }
    }
  }
}
