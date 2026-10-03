package com.springie.modification.segmentation;

import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.utilities.random.JUR;

public class Concatenate {
  JUR rnd = new JUR();

  int colour_link_geodesic = 0xFF00FF00;

  NodeManager node_manager;

  LinkManager link_manager;

  public Concatenate(NodeManager node_manager) {
    this.node_manager = node_manager;
    this.link_manager = node_manager.getLinkManager();
  }

  public void concatenate() {
    while (this.link_manager.isSelection()) {
      final Link link = this.link_manager.getFirstSelectedLink();
      concatenateFrom(link);
    }
  }

  private void concatenateFrom(final Link link) {
    // TODO Auto-generated method stub
  }
}
