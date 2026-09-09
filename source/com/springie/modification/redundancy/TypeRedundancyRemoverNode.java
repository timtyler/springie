package com.springie.modification.redundancy;

import java.util.ArrayList;

import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.elements.nodes.NodeType;

public class TypeRedundancyRemoverNode {
  NodeManager node_manager;

  public TypeRedundancyRemoverNode(NodeManager node_manager) {
    this.node_manager = node_manager;
  }

  public void removeRedundancy() {
    removeRedundancyInNodeTypes();
    this.node_manager.each_has_its_own_type = false;
  }

  public void removeRedundancyInNodeTypes() {
    final ArrayList<NodeType> v = this.node_manager.node_type_factory.array;
    final int size = v.size();
    for (int i = size; --i >= 0;) {
      final NodeType nt = v.get(i);
      if (equalspreviousNodeType(v, i)) {
        final int first = getFirstNodeType(v, nt, i);
        replaceNodeTypeWithPrevious(v, i, first);
      }
    }

    this.node_manager.each_has_its_own_type = false;
  }

  private void replaceNodeTypeWithPrevious(ArrayList<NodeType> v, int old, int nww) {
    final NodeType nt_old = v.get(old);
    final NodeType nt_nww = v.get(nww);
    final int n_o_n = this.node_manager.element.size();
    for (int i = n_o_n; --i >= 0;) {
      final Node n = (Node) this.node_manager.element.get(i);
      if (n.type.equals(nt_old)) {
        n.type = nt_nww;
      }
    }

    v.remove(old);
  }

  private int getFirstNodeType(ArrayList<NodeType> v, NodeType nt, int max) {
    for (int i = max; --i >= 0;) {
      if (nt.equals(v.get(i))) {
        return i;
      }
    }

    return -1;
  }

  private boolean equalspreviousNodeType(ArrayList<NodeType> v, int max) {
    final NodeType nt = v.get(max);

    return getFirstNodeType(v, nt, max) > 0;
  }
}
