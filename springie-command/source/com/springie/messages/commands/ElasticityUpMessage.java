//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.modification.CommandContextHolder;
import com.springie.modification.DomeRelatedChangeDelegator;
import com.springie.world.NewMessage;

public class ElasticityUpMessage extends NewMessage {
  public ElasticityUpMessage() {
    super(null);
  }

  public Object execute() {
    DomeRelatedChangeDelegator.elasticityUp();
    CommandContextHolder.get().reflectElasticity();

    return null;
  }
}
