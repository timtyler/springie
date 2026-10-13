//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.modification.CommandContextHolder;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.world.NewMessage;
import com.springie.modification.links.RemoveLinkAndFuseEnds;

public class RemoveLinkAndFuseEndsMessage extends NewMessage {
  public RemoveLinkAndFuseEndsMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();

    CommandContextHolder.get().prepareToModifyLinkTypes();
    final RemoveLinkAndFuseEnds end_fuse = new RemoveLinkAndFuseEnds(
      node_manager);
    end_fuse.action();
    CommandContextHolder.get().postCleanup();

    return null;
  }
}
