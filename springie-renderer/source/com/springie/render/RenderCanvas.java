// This code has been placed into the public domain by its author

package com.springie.render;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Panel;

/**
 * The canvas the renderer draws on. Implemented by MainCanvas (in core);
 * the renderer only needs these operations.
 */
public interface RenderCanvas {
  Panel getPanel();

  Image createImage(int x, int y);

  void forceResize();

  void update(Graphics g);

  void paint(Graphics g);
}
