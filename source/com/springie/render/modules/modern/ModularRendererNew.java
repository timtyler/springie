// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import java.awt.Graphics;
import java.util.Vector;

import com.springie.FrEnd;
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
  RendererBinManager bins_current = new RendererBinManager();

  RendererBinManager bins_last = new RendererBinManager();

  public static ObjectBase sphere_object = new SimpleDodecahedron();

  public void resize(int x, int y) {
    this.bins_current.resize(x, y);
    this.bins_last.resize(x, y);
  }

  public void reset() {
    this.bins_current.reset();
    this.bins_last.reset();
  }

  public void repaint(Graphics graphics, NodeManager manager) {
    // clear bins...

    this.bins_current.clear();

    final int mask = 0xFFFFFFFF;

    addNodesToBins(manager, mask);

    addLinksToBins(manager, mask);

    addFacesToBins(manager, mask);

    // do the drawing operations, offscreen if needed...
    
    this.bins_current.render(this.bins_last, graphics);

    // swap bin pointers

    RendererBinManager bins_temp = this.bins_current;
    this.bins_current = this.bins_last;
    this.bins_last = bins_temp;
  }

  private void addFacesToBins(NodeManager manager, int mask) {
    Forget.about(mask);
    if (FrEnd.render_faces) {
      final FaceManager face_manager = manager.getFaceManager();
      final Vector faces = face_manager.element;

      final int total_number = faces.size();

      for (int l = total_number; --l >= 0;) {
        final Face face = (Face) face_manager.element.elementAt(l);

        final PolygonComposite polygons = ElementRendererFace.getPolygon(face);

        this.bins_current.add(polygons);
      }
    }
  }

  private void addLinksToBins(NodeManager manager, int mask) {
    if (FrEnd.render_links) {
      final LinkManager link_manager = manager.getLinkManager();
      final int number = link_manager.element.size();

      for (int l = number; --l >= 0;) {
        final Link link = (Link) link_manager.element.elementAt(l);
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
            final Vector polygons = ElementRendererLink.getPolygon(link,
                node_1, node_2, link.getThicknesss(), colour);
            final int polygons_size = polygons.size();
            for (int pci = polygons_size; --pci >= 0;) {
              final PolygonComposite pc = (PolygonComposite) polygons.elementAt(pci);
              this.bins_current.add(pc);
            }
          }
        }
      }
    }
  }

  private void addNodesToBins(NodeManager manager, int mask) {
    Forget.about(mask);

    final int number_of_nodes = manager.element.size();

    if (FrEnd.render_nodes) {

      // fill bins...
      // ObjectBase sphere_object = new SimpleTetrahedron();
      // ObjectBase sphere_object = new SimpleOctahedron();
      // ObjectBase sphere_object = new SimpleIcosahedron();

      for (int counter = number_of_nodes; --counter >= 0;) {
        final Node node = (Node) manager.element.elementAt(counter);
        if (!node.type.hidden) {
          final PolygonComposite polygons = ElementRendererNode.get(
              ModularRendererNew.sphere_object, node);
          this.bins_current.add(polygons);
        }
      }
    }
  }
}
