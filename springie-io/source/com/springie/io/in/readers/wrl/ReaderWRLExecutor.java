// * Read in information from files...

package com.springie.io.in.readers.wrl;

import com.springie.io.in.LinkResetter;

import com.springie.elements.nodes.NodeManager;
import com.springie.elements.nodes.PostModification;
import com.springie.io.in.ScaleToFitScreen;
import com.springie.world.CentreOnScreen;
import com.tifsoft.utilities.execute.Executor;

public class ReaderWRLExecutor implements Executor {
  public Object execute(final Object o) {
    final NodeManager node_manager = (NodeManager) o;

    ScaleToFitScreen.scale(node_manager);
    CentreOnScreen.centre(node_manager);

    node_manager.getLinkManager().selectAll();
    
    new LinkResetter(node_manager).reset();

    node_manager.getLinkManager().deselectAll();
    
    new PostModification(node_manager).cleanup();

    return null;
  }
}
