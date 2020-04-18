// This code has been placed into the public domain by its author

package com.springie.render.modules;

import java.awt.Graphics;

import com.springie.elements.nodes.NodeManager;

public interface ModularRendererBase {
  void repaint(Graphics graphics, NodeManager manager);
  void resize(int x, int y);
  void reset();
}
