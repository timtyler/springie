package com.springie.elements.selection;

import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.lists.ListOfIntegers;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.modification.pre.PrepareToModifyNodeTypes;
import com.springie.render.RendererDelegator;

public class SelectionSpreader {
  NodeManager node_manager;

  LinkManager link_manager;

  public SelectionSpreader(NodeManager node_manager) {
    this.node_manager = node_manager;
    this.link_manager = node_manager.getLinkManager();
    prepare();
  }

  public void spreadSelectionViaLinks() {
    spreadFromNodesToLinks();
    spreadFromLinksToNodes();
    RendererDelegator.repaint_some_objects = true;
  }

  public void spreadFromLinksToNodes() {
    final int number = this.link_manager.element.size();
    for (int i = number; --i >= 0;) {
      final Link link = (Link) this.link_manager.element.elementAt(i);
      if (link.type.selected) {
        selectAllNodes(link);
      }
    }
  }

  public void spreadFromNodesToLinks() {
    final int number_of_nodes = this.node_manager.element.size();
    for (int counter = number_of_nodes; --counter >= 0;) {
      final Node candidate = (Node) this.node_manager.element
        .elementAt(counter);
      if (candidate.type.selected) {
        selectAllLinks(candidate);
      }
    }
  }

  private void selectAllNodes(Link link) {
    final Node[] nodes = link.nodes;
    final int total = nodes.length;
    for (int i = 0; i < total; i++) {
      if (!nodes[i].type.hidden) {
        nodes[i].type.selected = true;
      }
    }
  }

  private void selectAllLinks(Node node) {
    final ListOfIntegers list_of_links = node.list_of_links;
    final int n_o_l = list_of_links.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final int i = list_of_links.retreive(temp);
      final Link link = (Link) this.link_manager.element.elementAt(i);
      if (!link.type.hidden) {
        link.type.selected = true;
      }
    }
  }

  void prepare() {
    final PrepareToModifyNodeTypes prepare = new PrepareToModifyNodeTypes(
      this.node_manager);
    prepare.prepare();
  }
}
