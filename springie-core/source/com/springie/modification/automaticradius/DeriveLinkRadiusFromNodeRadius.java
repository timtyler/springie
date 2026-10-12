// This code has been placed into the public domain by its author
package com.springie.modification.automaticradius;

import com.springie.FrEnd;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.modification.pre.PrepareToModifyLinkTypes;

public class DeriveLinkRadiusFromNodeRadius {
  NodeManager node_manager;

  LinkManager link_manager;

  public DeriveLinkRadiusFromNodeRadius(NodeManager node_manager) {
    this.node_manager = node_manager;
    this.link_manager = node_manager.getLinkManager();
    prepare();
  }

  public void setInitially() {
    FrEnd.perform_selection.selectAll();
    set();
    FrEnd.perform_selection.deselectAll();
  }

  public void set() {
    final int n_o_l = this.link_manager.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link l = (Link) this.link_manager.element.get(temp);
      if (l.type.selected) {
        final int r1 = l.nodes[0].type.radius;
        final int r2 = l.nodes[01].type.radius;
        l.type.radius = (r1 + r2) / 3;
      }
    }
  }

  void prepare() {
    final PrepareToModifyLinkTypes prepare = new PrepareToModifyLinkTypes(
        this.node_manager.getLinkManager());
    prepare.prepare();
  }
}
