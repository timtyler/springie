//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.modification.CommandContextHolder;

import com.springie.FrEnd;
import com.springie.elements.links.Link;
import com.springie.world.NewMessage;

public class LinkLengthMessage extends NewMessage {
  private final int length;

  public LinkLengthMessage(int length) {
    super(null);
    this.length = length;
  }

  public Object execute() {
    CommandContextHolder.get().prepareToModifyLinkTypes();
    Link.link_display_length = this.length;
    CommandContextHolder.get().postCleanup();

    return null;
  }
}
