// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import java.awt.Graphics;
import java.util.List;
import java.util.ArrayList;

import com.springie.FrEnd;
import com.springie.render.RendererDelegator;
import com.springie.elements.faces.Face;
import com.springie.elements.faces.FaceManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.render.RendererDelegator;
import com.springie.render.modules.ModularRendererBase;
import com.tifsoft.Forget;

public class ModularRendererNew implements ModularRendererBase {
  RendererTileManager tiles_current = new RendererTileManager();

  RendererTileManager tiles_last = new RendererTileManager();

  public static ObjectBase sphere_object = new SimpleDodecahedron();

  /**
   * The frame's composites in creation order, reused across frames.
   * distribute() sorts them globally before tiling, so the tiles arrive
   * at render() pre-sorted and no per-tile sort is needed.
   */
  private final ArrayList<PolygonComposite> frame_composites =
      new ArrayList<>();

  public void resize(int x, int y) {
    this.tiles_current.resize(x, y);
    this.tiles_last.resize(x, y);
  }

  public void reset() {
    this.tiles_current.reset();
    this.tiles_last.reset();
  }

  public void repaint(Graphics graphics, NodeManager manager) {
    // clear tiles...

    this.tiles_current.clear();

    final ArrayList<PolygonComposite> all = this.frame_composites;
    all.clear();

    final int mask = 0xFFFFFFFF;

    addNodesToTiles(manager, mask, all);

    addLinksToTiles(manager, mask, all);

    addFacesToTiles(manager, mask, all);

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

  private void addFacesToTiles(NodeManager manager, int mask,
      ArrayList<PolygonComposite> all) {
    Forget.about(mask);
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

  private void addLinksToTiles(NodeManager manager, int mask,
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

  private void addNodesToTiles(NodeManager manager, int mask,
      ArrayList<PolygonComposite> all) {
    Forget.about(mask);

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
