//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.NewMessage;
import com.springie.modification.links.LinkSubdivider;

public class SplitLinksMessage extends NewMessage {
  public SplitLinksMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final LinkSubdivider ls = new LinkSubdivider(node_manager);
    final String ls_s = FrEnd.panel_edit_misc.textfield_split_links
      .getText();
    final int ls_i = Integer.parseInt(ls_s);
    final boolean b = FrEnd.panel_edit_misc.checkbox_split_links_remove_old
      .getState();

    ls.divide(ls_i, b);

    FrEnd.postCleanup();

    return null;
  }
}
