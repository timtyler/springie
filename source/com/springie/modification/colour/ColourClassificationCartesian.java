package com.springie.modification.colour;

import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.modification.pre.PrepareToModifyLinkClazzes;
import com.springie.render.RendererDelegator;

public class ColourClassificationCartesian {
  NodeManager node_manager;

  LinkManager link_manager;

  public ColourClassificationCartesian(NodeManager node_manager) {
    this.node_manager = node_manager;
    this.link_manager = node_manager.getLinkManager();

    prepare();
  }

  public void setColour() {
    setLinkColour();
  }

  void prepare() {
    final PrepareToModifyLinkClazzes prepare_ml = new PrepareToModifyLinkClazzes(
      this.node_manager);
    prepare_ml.prepare();
  }

  private void setLinkColour() {
    final int max_size = this.link_manager.element.size();

    for (int counter = max_size; --counter >= 0;) {
      final Link link = (Link) this.link_manager.element.elementAt(counter);
      if (link.type.selected) {
        int colour = 0xFF808080;
        final Node node1 = link.nodes[0];
        final Node node2 = link.nodes[link.nodes.length - 1];
        final int dx = node1.pos.x - node2.pos.x;
        final int dy = node1.pos.y - node2.pos.y;
        final int dz = node1.pos.z - node2.pos.z;
        if (dx != 0) {
          colour |= 0xFF0000;
        }
        if (dy != 0) {
          colour |= 0xFF00;
        }
        if (dz != 0) {
          colour |= 0xFF;
        }
        
        link.clazz.colour = colour;
      }
    }

    RendererDelegator.repaint_some_objects = true;
  }
}