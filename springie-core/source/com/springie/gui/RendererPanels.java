// This code has been placed into the public domain by its author

package com.springie.gui;

import com.springie.FrEnd;
import com.springie.gui.components.DropablePanel;
import com.springie.render.RendererDelegator;
import java.awt.Graphics;
import java.awt.Panel;

public final class RendererPanels {
  private RendererPanels() {
    // hide constructor...
  }

  public static Panel setUpPanelForFrame() {
    final Panel panel = new Panel() {
      static final long serialVersionUID = 1250;
      public void paint(final Graphics g) {
        RendererDelegator.canvas.paint(g);
      }

      public void update(final Graphics g) {
        RendererDelegator.canvas.update(g);
      }
    };
    return panel;
  }

  public static Panel setUpPanelForFrame2() {
    if (FrEnd.application && FrEnd.usingJava120()) {
      final DropablePanel panel = new DropablePanel() {
        static final long serialVersionUID = 1250;
        public void paint(final Graphics g) {
          RendererDelegator.canvas.paint(g);
        }

        public void update(final Graphics g) {
          RendererDelegator.canvas.update(g);
        }
      };
      return panel;
    }

    return setUpPanelForFrame();
  }
}
