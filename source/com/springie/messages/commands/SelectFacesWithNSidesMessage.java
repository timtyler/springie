//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.faces.FaceManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.NewMessage;

public class SelectFacesWithNSidesMessage extends NewMessage {
  public SelectFacesWithNSidesMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final FaceManager face_manager = node_manager.getFaceManager();

    FrEnd.prepareToModifyFaceTypes();
    final int n_sides = Integer
      .parseInt(FrEnd.panel_edit_select_advanced.textfield_select_faces_with_n_sides
        .getText());
    face_manager.selectAllWithNSides(n_sides);
    FrEnd.postCleanup();

    return null;
  }
}
