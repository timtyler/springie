package com.springie.modification.flags;

import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.modification.post.PostModification;
import com.springie.modification.pre.PrepareToModifyLinkTypes;

public class FlagControllerRope {
  LinkManager link_manager;

  NodeManager node_manager;

  public FlagControllerRope(NodeManager node_manager) {
    this.node_manager = node_manager;
    this.link_manager = node_manager.getLinkManager();
    prepare();
  }

  public void rope(boolean compression, boolean tension) {
    ropeLinks(compression, tension);
    new PostModification(this.node_manager).cleanup();
  }

  private void ropeLinks(boolean compression, boolean tension) {
    final int n_o_l = this.link_manager.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link l = (Link) this.link_manager.element.get(temp);
      if (l.type.selected) {
        l.type.compression = compression;
        l.type.tension = tension;
      }
    }
  }

  void prepare() {
    final PrepareToModifyLinkTypes prepare_l = new PrepareToModifyLinkTypes(
      this.link_manager);
    prepare_l.prepare();
  }
}

