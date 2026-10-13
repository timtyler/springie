//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.modification.CommandContextHolder;

import com.springie.context.ContextManager;
import com.springie.elements.faces.FaceManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.world.NewMessage;

public class SelectFacesWithNSidesMessage extends NewMessage {
  public SelectFacesWithNSidesMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final FaceManager face_manager = node_manager.getFaceManager();

    CommandContextHolder.get().prepareToModifyFaceTypes();
    final int n_sides = Integer
      .parseInt(CommandContextHolder.get().getSelectFacesWithNSides());
    face_manager.selectAllWithNSides(n_sides);
    CommandContextHolder.get().postCleanup();

    return null;
  }
}
