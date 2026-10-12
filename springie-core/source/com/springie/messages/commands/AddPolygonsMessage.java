//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.NewMessage;
import com.springie.modification.faces.FaceMaker;

public class AddPolygonsMessage extends NewMessage {
  public AddPolygonsMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final FaceMaker polygon_maker = new FaceMaker(node_manager);
    //final int c = FrEnd.colour_picker.getColour();
    final int i = Integer
      .parseInt(FrEnd.panel_edit_misc.textfield_add_polygons.getText());
    polygon_maker.addPolygons(i);

    FrEnd.postCleanup();

    return null;
  }
}
