//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.NewMessage;
import com.springie.modification.automaticradius.AutomaticNodeRadius;

public class AutomaticNodeRadiusMessage extends NewMessage {
  public AutomaticNodeRadiusMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final AutomaticNodeRadius anr = new AutomaticNodeRadius(node_manager);
    final String anr_s = FrEnd.panel_edit_misc.textfield_automatic_node_radius
      .getText();
    final double anr_d = Double.parseDouble(anr_s);
    anr.set(anr_d, false);
    FrEnd.postCleanup();

    return null;
  }
}
