//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.demos.CompassPoint;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.NewMessage;

public class AlterCompassMessage extends NewMessage {
  private final CompassPoint heading;

  public AlterCompassMessage(CompassPoint heading) {
    super(null);
    this.heading = heading;
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();

    node_manager.setCompassOfSelected(this.heading);
    FrEnd.panel_edit_properties_scalars.reflectCompass();
    FrEnd.reflectValuesInGUIAfterPropertyEditing();

    return null;
  }
}
