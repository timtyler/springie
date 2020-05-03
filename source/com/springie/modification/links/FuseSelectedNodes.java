// This code has been placed into the public domain by its author
package com.springie.modification.links;

import com.springie.FrEnd;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.lists.ListOfIntegers;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.modification.post.PostModification;
import com.springie.modification.pre.PrepareToModifyLinkTypes;
import com.springie.modification.pre.PrepareToModifyNodeTypes;

public class FuseSelectedNodes {
  NodeManager node_manager;

  LinkManager link_manager;

  PostModification post_modification;

  public FuseSelectedNodes(NodeManager node_manager) {
    this.node_manager = node_manager;
    this.link_manager = node_manager.getLinkManager();
    this.post_modification = new PostModification(node_manager);

    prepare();
  }

  public void action() {
    fuse();

    FrEnd.postCleanup();
  }

  public void fuse() {
    final Node n1 = this.node_manager.getSelectedNode();

    fuseThisNode(n1);
  }

  private void fuseThisNode(final Node n1) {
    new PostModification(this.node_manager).generateListOfLinks();
    final int n_o_n = this.node_manager.element.size();
    for (int temp = n_o_n; --temp >= 0;) {
      final Node node = (Node) this.node_manager.element.get(temp);
      if (node.type.selected) {
        if (node != n1) {
          final ListOfIntegers lol = node.list_of_links;
          final int lol_l = lol.size();
          // loop through the links...
          for (int i = 0; i < lol_l; i++) {
            final int idx = lol.retreive(i);
            final Link lk = (Link) this.link_manager.element.get(idx);

            //if ((lk.nodes[0] == n1) || (lk.nodes[1] == n1)) {
              // kill any link between them...
              //this.link_manager.killSpecifiedLink(lk);              
            //} else {
              if (lk.nodes[0] == node) {
                makeLink(n1, lk.nodes[1], lk);
              } else if (lk.nodes[1] == node) {
                makeLink(n1, lk.nodes[0], lk);
              }
            //}
          }
          this.node_manager.killThisNode(node);
          new PostModification(this.node_manager).generateListOfLinks();
        }
      }
    }
  }

  private void makeLink(final Node n1, final Node n2, final Link lk) {
    this.link_manager.setLink(n1, n2, lk.type, lk.clazz);
  }

  void prepare() {
    final PrepareToModifyLinkTypes prepare_l = new PrepareToModifyLinkTypes(
        this.node_manager.getLinkManager());
    prepare_l.prepare();

    final PrepareToModifyNodeTypes prepare_n = new PrepareToModifyNodeTypes(
        this.node_manager);
    prepare_n.prepare();
  }
}