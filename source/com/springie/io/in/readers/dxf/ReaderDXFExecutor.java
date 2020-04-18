// * Read in information from files...

package com.springie.io.in.readers.dxf;

import com.springie.elements.nodes.NodeManager;
import com.springie.modification.post.PostModification;
import com.springie.modification.resize.LinkResetter;
import com.springie.modification.resize.ScaleToFitScreen;
import com.springie.modification.translation.CentreOnScreen;
import com.tifsoft.utilities.execute.Executor;

public class ReaderDXFExecutor implements Executor {
  public Object execute(Object o) {
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
