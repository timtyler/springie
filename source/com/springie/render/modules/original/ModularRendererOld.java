// This code has been placed into the public domain by its author

package com.springie.render.modules.original;


import java.awt.Graphics;

import com.springie.FrEnd;
import com.springie.elements.nodes.NodeManager;
import com.springie.render.modules.ModularRendererBase;
import com.tifsoft.Forget;

public class ModularRendererOld implements ModularRendererBase {
  public void repaint(Graphics graphics, NodeManager manager) {
    Forget.about(graphics);
    nodeAndLinkRender(manager);
  }

  public void nodeAndLinkRender(NodeManager manager) {
    manager.sortIndex();

    if (FrEnd.render_anaglyph) {
      manager.nodeAndLinkRenderAnaglyph();
    } else {
      manager.nodeAndLinkRenderNormal();
    }
  }

  public void resize(int x, int y) {
    Forget.about(x);
    Forget.about(y);
    reset();
  }

  public void reset() {
    //...
  }
}
