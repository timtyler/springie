//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.modification.CommandContextHolder;

import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.world.NewMessage;
import com.springie.io.in.AutomaticNodeRadius;

public class AutomaticNodeRadiusMessage extends NewMessage {
  public AutomaticNodeRadiusMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final AutomaticNodeRadius anr = new AutomaticNodeRadius(node_manager);
    final String anr_s = CommandContextHolder.get().getAutomaticNodeRadius();
    final double anr_d = Double.parseDouble(anr_s);
    anr.set(anr_d, false);
    CommandContextHolder.get().postCleanup();

    return null;
  }
}
