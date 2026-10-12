package com.springie.modification.delete;

import com.springie.FrEnd;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.modification.pre.PrepareToModifyLinkTypes;
import com.springie.modification.pre.PrepareToModifyNodeTypes;
import com.springie.render.RendererDelegator;

public class DeleteLinks {
  NodeManager node_manager;

  public DeleteLinks(NodeManager node_manager) {
    this.node_manager = node_manager;
    
    prepare();
  }

  public void delete() {
    final int number_of_nodes = this.node_manager.element.size();

    for (int counter = number_of_nodes; --counter >= 0;) {
      final Node candidate = (Node) this.node_manager.element.get(counter);
      if (candidate.type.selected) {
        FrEnd.killAllLinks(candidate);
      }
    }
    
    RendererDelegator.repaintAll();
  }
  
  public void prepare() {
    final PrepareToModifyNodeTypes prepare_n = new PrepareToModifyNodeTypes(this.node_manager);
    prepare_n.prepare();
    final PrepareToModifyLinkTypes prepare_l = new PrepareToModifyLinkTypes(this.node_manager.getLinkManager());
    prepare_l.prepare();
  }
}
