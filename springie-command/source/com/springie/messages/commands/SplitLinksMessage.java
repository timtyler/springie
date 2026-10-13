//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.modification.CommandContextHolder;

import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.world.NewMessage;
import com.springie.modification.links.LinkSubdivider;

public class SplitLinksMessage extends NewMessage {
  public SplitLinksMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final LinkSubdivider ls = new LinkSubdivider(node_manager);
    final String ls_s = CommandContextHolder.get().getSplitLinks();
    final int ls_i = Integer.parseInt(ls_s);
    final boolean b = CommandContextHolder.get().getSplitLinksRemoveOld();

    ls.divide(ls_i, b);

    CommandContextHolder.get().postCleanup();

    return null;
  }
}
