//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.messages.NewMessage;
import com.springie.modification.DomeRelatedChangeDelegator;

public class ElasticityUpMessage extends NewMessage {
  public ElasticityUpMessage() {
    super(null);
  }

  public Object execute() {
    DomeRelatedChangeDelegator.elasticityUp();
    FrEnd.panel_edit_properties_scalars.reflectElasticity();

    return null;
  }
}
