// This code has been placed into the public domain by its author

package com.springie.render;

import java.awt.Graphics;
import java.awt.Panel;

import com.springie.FrEnd;
import com.springie.gui.components.DropablePanel;

public final class RendererPanels {
  private RendererPanels() {
    // hide constructor...
  }

  public static Panel setUpPanelForFrame() {
    final Panel panel = new Panel() {
      static final long serialVersionUID = 1250;
      public void paint(Graphics g) {
        FrEnd.main_canvas.paint(g);
      }

      public void update(Graphics g) {
        FrEnd.main_canvas.update(g);
      }
    };
    return panel;
  }

  public static Panel setUpPanelForFrame2() {
    if (FrEnd.application && FrEnd.usingJava120()) {
      final DropablePanel panel = new DropablePanel() {
        static final long serialVersionUID = 1250;
        public void paint(Graphics g) {
          FrEnd.main_canvas.paint(g);
        }

        public void update(Graphics g) {
          FrEnd.main_canvas.update(g);
        }
      };
      return panel;
    }

    return setUpPanelForFrame();
  }
}
