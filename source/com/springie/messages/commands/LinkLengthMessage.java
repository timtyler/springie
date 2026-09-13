//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.elements.links.Link;
import com.springie.messages.NewMessage;

public class LinkLengthMessage extends NewMessage {
  private final int length;

  public LinkLengthMessage(int length) {
    super(null);
    this.length = length;
  }

  public Object execute() {
    FrEnd.prepareToModifyLinkTypes();
    Link.link_display_length = this.length;
    FrEnd.postCleanup();

    return null;
  }
}
