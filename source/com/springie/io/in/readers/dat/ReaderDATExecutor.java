// * Read in information from files...

package com.springie.io.in.readers.dat;

import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.io.in.AutomaticNodeColourer;
import com.springie.modification.links.FuseSelectedNodes;
import com.springie.modification.post.PostModification;
import com.springie.modification.pre.PrepareToModifyLinkTypes;
import com.springie.modification.pre.PrepareToModifyNodeTypes;
import com.springie.modification.resize.LinkResetter;
import com.springie.modification.resize.ScaleToFitScreen;
import com.springie.modification.translation.CentreOnScreen;
import com.springie.utilities.log.Log;
import com.tifsoft.utilities.execute.Executor;

public class ReaderDATExecutor implements Executor {
  public Object execute(Object o) {
    final NodeManager node_manager = (NodeManager) o;

    ScaleToFitScreen.scale(node_manager);
    CentreOnScreen.centre(node_manager);

    eliminateAnyDuplicateNodes(node_manager);

    // select all links...
    node_manager.getLinkManager().selectAll();

    new LinkResetter(node_manager).reset();

    node_manager.getLinkManager().deselectAll();

    new PostModification(node_manager).cleanup();

    new AutomaticNodeColourer(node_manager).execute();

    return null;
  }

  private void eliminateAnyDuplicateNodes(NodeManager node_manager) {
    prepare(node_manager);
    final int n_o_n = node_manager.element.size();
    boolean finished;
    int index = 0;
    do {
      finished = processDuplicateNodes(node_manager);
      Log.log("Deduping (" + index++ + "/" + n_o_n + ")");
      node_manager.deselectAll();
    } while (!finished);
    new PostModification(node_manager).cleanup();
  }

  private boolean processDuplicateNodes(NodeManager node_manager) {
    new PostModification(node_manager).generateListOfLinks();
    final int n_o_n = node_manager.element.size();
    final boolean finished = true;
    for (int idx_1 = n_o_n; --idx_1 >= 0;) {
      for (int idx_2 = idx_1; --idx_2 >= 0;) {
        if (finished) {
          final Node node_1 = (Node) node_manager.element.get(idx_1);
          final Node node_2 = (Node) node_manager.element.get(idx_2);
          if (node_1.pos.equals(node_2.pos)) {
            node_1.type.selected = true;
            node_2.type.selected = true;
            new FuseSelectedNodes(node_manager).fuse();
            return false;
          }
        }
      }
    }
    return true;
  }
  
  void prepare(NodeManager node_manager) {
    final PrepareToModifyLinkTypes prepare_l = new PrepareToModifyLinkTypes(
        node_manager.getLinkManager());
    prepare_l.prepare();

    final PrepareToModifyNodeTypes prepare_n = new PrepareToModifyNodeTypes(
        node_manager);
    prepare_n.prepare();
  }
}
