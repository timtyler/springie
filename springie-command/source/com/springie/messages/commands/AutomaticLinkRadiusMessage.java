//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.modification.CommandContextHolder;

import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.world.NewMessage;
import com.springie.io.in.AutomaticLinkRadius;

public class AutomaticLinkRadiusMessage extends NewMessage {
  public AutomaticLinkRadiusMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final AutomaticLinkRadius alr = new AutomaticLinkRadius(node_manager);
    final String alr_s = CommandContextHolder.get().getAutomaticLinkRadius();
    final double alr_d = Double.parseDouble(alr_s);
    alr.set(alr_d);
    CommandContextHolder.get().postCleanup();

    return null;
  }
}
