package com.springie.metrics;

import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.NodeManager;

public class AverageLengthGetter {
  NodeManager node_manager;

  LinkManager link_manager;

  public AverageLengthGetter(NodeManager node_manager) {
    if (node_manager == null) {
      return;
    }
    this.node_manager = node_manager;
    this.link_manager = node_manager.getLinkManager();
  }

  public int getAverage() {
    if (this.node_manager == null) {
      return 0;
    }
    final int number = this.link_manager.element.size();
    int count = 0;
    long total_length = 0;
    for (int counter = number; --counter >= 0;) {
      final Link candidate = (Link) this.link_manager.element
          .get(counter);
      if (candidate.type.selected) {
        total_length += candidate.type.length;
        count++;
      }
    }

    if (count == 0) {
      return 0;
    }

    return (int) (total_length / count);
  }
}