package com.springie.gui.panels.controls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.Component;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Label;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.springie.FrEnd;

/**
 * When nothing is selected, the Properties > Scalars tab must say so
 * instead of showing an empty panel.
 */
class PanelControlsPropertiesScalarsTest {

  private static Component[] scalarsChildren() throws Exception {
    final Component[][] holder = new Component[1][];
    SwingUtilities.invokeAndWait(() -> holder[0] =
        FrEnd.panel_edit_properties_scalars.panel.getComponents());
    return holder[0];
  }

  @Test
  void showsPromptWhenNothingIsSelected() throws Exception {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");

    SwingUtilities.invokeAndWait(() -> FrEnd.main(new String[0]));
    try {
      SwingUtilities.invokeAndWait(() -> FrEnd.panel_edit_properties_scalars
          .resetPanel(false, false, false));

      final Component[] children = scalarsChildren();
      assertEquals(1, children.length,
          "scalars panel should show exactly the prompt when nothing is selected");
      assertTrue(children[0] instanceof Label,
          "the prompt should be a label");
      assertEquals("Nothing is selected. Make a selection.",
          ((Label) children[0]).getText());

      // With a selection the prompt must be gone.
      SwingUtilities.invokeAndWait(() -> FrEnd.panel_edit_properties_scalars
          .resetPanel(true, true, false));

      final Component[] with_selection = scalarsChildren();
      assertEquals(5, with_selection.length,
          "radius, length, elasticity, damping and charge panels expected");
      for (final Component child : with_selection) {
        assertTrue(!(child instanceof Label)
            || !((Label) child).getText().contains("Nothing is selected"),
            "prompt must not linger once something is selected");
      }
    } finally {
      for (final Frame frame : Frame.getFrames()) {
        frame.dispose();
      }
    }
  }
}
