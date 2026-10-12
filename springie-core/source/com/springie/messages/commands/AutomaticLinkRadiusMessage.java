//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.NewMessage;
import com.springie.modification.automaticradius.AutomaticLinkRadius;

public class AutomaticLinkRadiusMessage extends NewMessage {
  public AutomaticLinkRadiusMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final AutomaticLinkRadius alr = new AutomaticLinkRadius(node_manager);
    final String alr_s = FrEnd.panel_edit_misc.textfield_automatic_link_radius
      .getText();
    final double alr_d = Double.parseDouble(alr_s);
    alr.set(alr_d);
    FrEnd.postCleanup();

    return null;
  }
}
