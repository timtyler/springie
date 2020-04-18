package com.springie.io.out;

import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;

public class DuplicateLinkRemover {
  NodeManager node_manager;

  LinkManager link_manager;

  public DuplicateLinkRemover(NodeManager node_manager) {
    this.node_manager = node_manager;
    this.link_manager = node_manager.getLinkManager();
  }

  public void removeDuplicateLinks() {
    final LinkManager link_manager = this.link_manager;

    final int n = link_manager.element.size();

    for (int i = n; --i >= 0;) {
      final Link l1 = (Link) link_manager.element.elementAt(i);
      for (int j = i - 1; --j >= 0;) {
        final Link l2 = (Link) link_manager.element.elementAt(j);
        if (linkHasTheSameNodesInASimilarOrder(l1, l2)) {
          link_manager.killNumberedLink(i);
        }
      }
    }
  }

  private boolean linkHasTheSameNodesInASimilarOrder(Link l1, Link l2) {
    if (linkHasTheSameNodesInTheSameOrder(l1, l2)) {
      return true;
    }

    if (linkHasTheSameNodesInTheReverseOrder(l1, l2)) {
      return true;
    }

    return false;
  }

  private boolean linkHasTheSameNodesInTheSameOrder(Link l1, Link l2) {
    final int total = l1.nodes.length;

    if (total != l2.nodes.length) {
      return false;
    }

    final Node[] n1 = l1.nodes;
    final Node[] n2 = l2.nodes;
    for (int i = 0; i < total; i++) {
      if (n1[i] != n2[i]) {
        return false;
      }
    }

    return true;
  }

  private boolean linkHasTheSameNodesInTheReverseOrder(Link l1, Link l2) {
    final int total = l1.nodes.length;

    if (total != l2.nodes.length) {
      return false;
    }

    final Node[] n1 = l1.nodes;
    final Node[] n2 = l2.nodes;
    for (int i = 0; i < total; i++) {
      if (n1[i] != n2[total - 1 - i]) {
        return false;
      }
    }

    return true;
  }
}
