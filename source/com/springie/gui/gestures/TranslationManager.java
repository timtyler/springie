// This code has been placed into the public domain by its author

package com.springie.gui.gestures;

import com.springie.FrEnd;
import com.springie.context.ContextMananger;
import com.springie.elements.nodes.Node;
import com.springie.geometry.Point3D;
import com.springie.render.RendererDelegator;

public class TranslationManager {
  private int start_x;

  private int start_y;

  Point3D[] pos;

  public void initialise(int x, int y) {
    if (FrEnd.button_virginity) {
      this.start_x = x;
      this.start_y = y;

      this.pos = new TransferUtilities().transferPositions();
      FrEnd.forces_disabled_during_gesture = true;
    } else {
      performTranslation(x, y);
    }
  }

  public void performTranslation(int x, int y) {
    final int n = ContextMananger.getNodeManager().element.size();

    for (int i = n; --i >= 0;) {
      final Node node = (Node) ContextMananger.getNodeManager().element.elementAt(i);

      final Point3D point = this.pos[i];
      node.pos.x = point.x + ((x - this.start_x) >> 0);
      node.pos.y = point.y + ((y - this.start_y) >> 0);
      node.pos.z = point.z;
    }
    
    RendererDelegator.repaint_some_objects = true;
//    FrEnd.postCleanup();
  }

  public void terminate(int x, int y) {
    if (this.pos != null) {
      performTranslation(x, y);
      this.pos = null;
      FrEnd.forces_disabled_during_gesture = false;
    }
  }
}