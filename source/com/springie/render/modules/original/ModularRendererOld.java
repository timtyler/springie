// This code has been placed into the public domain by its author

package com.springie.render.modules.original;


import com.springie.FrEnd;
import com.springie.elements.nodes.NodeManager;
import com.springie.render.RendererDelegator;
import com.springie.render.modules.ModularRendererBase;
import java.awt.Graphics;

public class ModularRendererOld implements ModularRendererBase {
  public void repaint(final Graphics graphics, final NodeManager manager) {
    nodeAndLinkRender(manager);
  }

  public void nodeAndLinkRender(final NodeManager manager) {
    manager.sortIndex();

    if (FrEnd.render_anaglyph) {
      manager.nodeAndLinkRenderAnaglyph();
    } else {
      manager.nodeAndLinkRenderNormal();
    }

    RendererDelegator.countRenderedFrame();
  }

  public void resize(final int x, final int y) {
    reset();
  }

  public void reset() {
    //...
  }
}
