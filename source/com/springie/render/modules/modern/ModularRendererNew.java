// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import com.springie.FrEnd;
import com.springie.elements.faces.Face;
import com.springie.elements.faces.FaceManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.gui.gestures.DragBoxManager;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;
import com.springie.render.modules.ModularRendererBase;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

public class ModularRendererNew implements ModularRendererBase {
  RendererTileManager tiles_current = new RendererTileManager();

  RendererTileManager tiles_last = new RendererTileManager();

  public static ObjectBase sphere_object = new SimpleC60();

  /**
   * The frame's composites in creation order, reused across frames.
   * distribute() sorts them globally before tiling, so the tiles arrive
   * at render() pre-sorted and no per-tile sort is needed.
   */
  private final ArrayList<PolygonComposite> frame_composites =
      new ArrayList<>();

  /**
   * Last frame's dirty rectangle in direct-to-canvas mode (Tim, 2026-10-04):
   * the union of content bounds, used to clear moved content.
   */
  private final com.springie.render.RectangleInt last_direct_dirty =
      new com.springie.render.RectangleInt(Integer.MAX_VALUE,
          Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);

  private boolean direct_first_frame = true;

  public void resize(final int x, final int y) {
    this.tiles_current.resize(x, y);
    this.tiles_last.resize(x, y);
  }

  public void reset() {
    this.tiles_current.reset();
    this.tiles_last.reset();
  }

  public void repaint(final Graphics graphics, final NodeManager manager) {
    // Single buffering (Tim, 2026-10-04): render directly to the canvas,
    // no tiles.
    if (RendererTileManager.direct_to_canvas) {
      repaintDirect(graphics, manager);
      return;
    }
    // clear tiles...

    this.tiles_current.clear();

    final ArrayList<PolygonComposite> all = this.frame_composites;
    all.clear();

    final int mask = 0xFFFFFFFF;

    addNodesToTiles(manager, mask, all);

    addLinksToTiles(manager, mask, all);

    addFacesToTiles(manager, mask, all);

    addDragBoxToTiles(all);

    // One global depth sort, then distribute to the tiles in sorted
    // order: every tile's vector arrives at render() pre-sorted, so the
    // per-tile sorts are gone.
    this.tiles_current.distribute(all, FrEnd.redraw_deepest_first);

    // do the drawing operations, offscreen if needed...

    this.tiles_current.render(this.tiles_last, graphics);

    // Rotate per-tile frame state: tiles_last takes this frame's vectors and
    // rectangles for next frame's damage repair (moved content is scrubbed
    // over the union of last frame's and this frame's content rects).
    this.tiles_current.rotateFrameState(this.tiles_last);

    RendererDelegator.countRenderedFrame();
  }

  /**
   * Single-buffered repaint (Tim, 2026-10-04): renders the dirty
   * rectangle directly onto the main canvas Graphics, with no tile
   * images. The main canvas is the only buffer.
   */
  private void repaintDirect(final Graphics graphics,
      final NodeManager manager) {
    final ArrayList<PolygonComposite> all = this.frame_composites;
    all.clear();

    final int mask = 0xFFFFFFFF;
    addNodesToTiles(manager, mask, all);
    addLinksToTiles(manager, mask, all);
    addFacesToTiles(manager, mask, all);
    addDragBoxToTiles(all);

    // Depth sort (same as distribute(), but without tiling).
    final int size = all.size();
    if (FrEnd.redraw_deepest_first && size > 1) {
      // Simple insertion sort by z (stable, adequate for direct mode).
      for (int i = 1; i < size; i++) {
        final PolygonComposite key = all.get(i);
        final int key_z = key.z;
        int j = i - 1;
        while (j >= 0 && all.get(j).z > key_z) {
          all.set(j + 1, all.get(j));
          j--;
        }
        all.set(j + 1, key);
      }
    }

    // Compute the dirty rectangle: union of this frame's content bounds
    // and last frame's (to clear moved content).
    final com.springie.render.RectangleInt dirty =
        new com.springie.render.RectangleInt(Integer.MAX_VALUE,
            Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
    for (int i = 0; i < size; i++) {
      final com.springie.render.RectangleInt bb =
          all.get(i).getBoundingBox();
      if (bb.min_x < dirty.min_x) dirty.min_x = bb.min_x;
      if (bb.min_y < dirty.min_y) dirty.min_y = bb.min_y;
      if (bb.max_x > dirty.max_x) dirty.max_x = bb.max_x;
      if (bb.max_y > dirty.max_y) dirty.max_y = bb.max_y;
    }
    // Union with last frame's dirty rect.
    if (!this.direct_first_frame) {
      final com.springie.render.RectangleInt last = this.last_direct_dirty;
      if (last.min_x < dirty.min_x) dirty.min_x = last.min_x;
      if (last.min_y < dirty.min_y) dirty.min_y = last.min_y;
      if (last.max_x > dirty.max_x) dirty.max_x = last.max_x;
      if (last.max_y > dirty.max_y) dirty.max_y = last.max_y;
    }
    this.direct_first_frame = false;
    // Save for next frame.
    this.last_direct_dirty.min_x = dirty.min_x;
    this.last_direct_dirty.min_y = dirty.min_y;
    this.last_direct_dirty.max_x = dirty.max_x;
    this.last_direct_dirty.max_y = dirty.max_y;

    final int width = com.springie.render.Coords.x_pixels;
    final int height = com.springie.render.Coords.y_pixels;
    // Clamp to canvas.
    int x0 = Math.max(0, dirty.min_x);
    int y0 = Math.max(0, dirty.min_y);
    int x1 = Math.min(width - 1, dirty.max_x);
    int y1 = Math.min(height - 1, dirty.max_y);
    if (x0 > x1 || y0 > y1) {
      return; // Nothing to draw.
    }

    // Clear the dirty rectangle with the background colour.
    graphics.setColor(new java.awt.Color(
        RendererDelegator.color_background_number));
    graphics.fillRect(x0, y0, x1 - x0 + 1, y1 - y0 + 1);

    // Draw the composites directly, clipped to the dirty rect.
    final java.awt.Shape old_clip = graphics.getClip();
    graphics.setClip(x0, y0, x1 - x0 + 1, y1 - y0 + 1);
    try {
      // Draw back-to-front (reverse of sorted order) for correct overlap.
      for (int i = size; --i >= 0;) {
        this.tiles_current.renderThePolygon(graphics, all.get(i));
      }
    } finally {
      graphics.setClip(old_clip);
    }

    RendererDelegator.countRenderedFrame();
  }

  /**
   * Adds the drag-box selection rectangle to the tiled render as geometry.
   * The box is drawn into the tile images (not as a screen-space overlay),
   * so the tiled renderer's normal damage repair covers the old rectangle
   * when it moves -- no trails, even with show_tiles gaps.
   */
  private void addDragBoxToTiles(final ArrayList<PolygonComposite> all) {
    final DragBoxManager drag_box_manager =
        FrEnd.perform_actions.drag_box_manager;
    if (drag_box_manager == null
        || drag_box_manager.drag_box_start == null
        || drag_box_manager.drag_box_end == null) {
      return;
    }

    final int x0 = Coords.getPixelFromInternalCoords(
        Math.min(drag_box_manager.drag_box_start.x,
            drag_box_manager.drag_box_end.x));
    final int x1 = Coords.getPixelFromInternalCoords(
        Math.max(drag_box_manager.drag_box_start.x,
            drag_box_manager.drag_box_end.x));
    final int y0 = Coords.getPixelFromInternalCoords(
        Math.min(drag_box_manager.drag_box_start.y,
            drag_box_manager.drag_box_end.y));
    final int y1 = Coords.getPixelFromInternalCoords(
        Math.max(drag_box_manager.drag_box_start.y,
            drag_box_manager.drag_box_end.y));

    // 3px thick lines, matching RendererDragBox.drawThickLine.
    final int t = 3;
    final int colour = RendererDelegator.colour_selected.getRGB();
    // On top of everything.
    final int z = Integer.MAX_VALUE;

    // Left, top, bottom, right edges as filled rectangles.
    all.add(new PolygonComposite(new PolygonObject2D[] {
        new PolygonObject2D(
            new int[] {x0 - t, x0 + t, x0 + t, x0 - t, },
            new int[] {y0 - t, y0 - t, y1 + t, y1 + t, },
            colour),
    }, z));
    all.add(new PolygonComposite(new PolygonObject2D[] {
        new PolygonObject2D(
            new int[] {x0 - t, x1 + t, x1 + t, x0 - t, },
            new int[] {y0 - t, y0 - t, y0 + t, y0 + t, },
            colour),
    }, z));
    all.add(new PolygonComposite(new PolygonObject2D[] {
        new PolygonObject2D(
            new int[] {x0 - t, x1 + t, x1 + t, x0 - t, },
            new int[] {y1 - t, y1 - t, y1 + t, y1 + t, },
            colour),
    }, z));
    all.add(new PolygonComposite(new PolygonObject2D[] {
        new PolygonObject2D(
            new int[] {x1 - t, x1 + t, x1 + t, x1 - t, },
            new int[] {y0 - t, y0 - t, y1 + t, y1 + t, },
            colour),
    }, z));
  }

  private void addFacesToTiles(final NodeManager manager, final int mask,
      ArrayList<PolygonComposite> all) {
    if (FrEnd.render_faces) {
      final FaceManager face_manager = manager.getFaceManager();
      final List<Face> faces = face_manager.element;

      final int total_number = faces.size();

      for (int l = total_number; --l >= 0;) {
        final Face face = face_manager.element.get(l);

        final PolygonComposite polygons = ElementRendererFace.getPolygon(face);

        all.add(polygons);
      }
    }
  }

  private void addLinksToTiles(final NodeManager manager, final int mask,
      ArrayList<PolygonComposite> all) {
    if (FrEnd.render_links) {
      final LinkManager link_manager = manager.getLinkManager();
      final int number = link_manager.element.size();

      for (int l = number; --l >= 0;) {
        final Link link = (Link) link_manager.element.get(l);
        if (!link.type.hidden) {
          final Node[] nodes = link.nodes;
          final int node_n = nodes.length;
          int colour = link.clazz.colour & mask;
          for (int i = 0; i < (node_n - 1); i++) {
            final Node node_1 = nodes[i];
            final Node node_2 = nodes[i + 1];
            if (link.type.selected) {
              colour = RendererDelegator.colour_selected_number;
            }
            final ArrayList<PolygonComposite> polygons = ElementRendererLink.getPolygon(link,
                node_1, node_2, link.getThicknesss(), colour);
            final int polygons_size = polygons.size();
            for (int pci = polygons_size; --pci >= 0;) {
              final PolygonComposite pc = polygons.get(pci);
              all.add(pc);
            }
          }
        }
      }
    }
  }

  private void addNodesToTiles(final NodeManager manager, final int mask,
      ArrayList<PolygonComposite> all) {

    final int number_of_nodes = manager.element.size();

    if (FrEnd.render_nodes) {

      // fill tiles...
      // ObjectBase sphere_object = new SimpleTetrahedron();
      // ObjectBase sphere_object = new SimpleOctahedron();
      // ObjectBase sphere_object = new SimpleIcosahedron();

      for (int counter = number_of_nodes; --counter >= 0;) {
        final Node node = (Node) manager.element.get(counter);
        if (!node.type.hidden) {
          final PolygonComposite polygons = ElementRendererNode.get(
              ModularRendererNew.sphere_object, node);
          all.add(polygons);
        }
      }
    }
  }
}
