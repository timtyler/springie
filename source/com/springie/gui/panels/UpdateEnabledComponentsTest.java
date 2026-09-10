package com.springie.gui.panels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.Frame;
import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.context.ContextMananger;

/**
 * Regression test: toggling one of the Nodes/Links/Faces checkboxes in the
 * select panel must not freeze the Properties panels.
 *
 * Previously UpdateEnabledComponents.actuallyUpdate() cleared needs_update
 * instead of buttons_need_update in the buttons branch. Once
 * buttons_need_update was set, every subsequent selection change had its GUI
 * refresh (the resetPanel rebuild of the Scalars/Flags panels) silently
 * discarded, so Properties > Scalars stayed empty no matter what was
 * selected.
 */
class UpdateEnabledComponentsTest {

  private static int scalarsChildCount() throws Exception {
    final int[] count = new int[1];
    SwingUtilities.invokeAndWait(() -> count[0] =
        FrEnd.panel_edit_properties_scalars.panel.getComponentCount());
    return count[0];
  }

  @Test
  void scalarsPanelPopulatesAfterCheckboxToggleAndLinkSelection()
      throws Exception {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");

    SwingUtilities.invokeAndWait(() -> FrEnd.main(new String[0]));
    try {
      // Wait for the startup model to load.
      final long loaded_by = System.currentTimeMillis() + 30000;
      while (ContextMananger.getLinkManager() == null
          || ContextMananger.getLinkManager().element.isEmpty()) {
        if (System.currentTimeMillis() > loaded_by) {
          throw new IllegalStateException("startup model did not load");
        }
        Thread.sleep(500);
      }

      // Simulate the user toggling one of the Nodes/Links/Faces checkboxes
      // in the select panel.
      SwingUtilities.invokeAndWait(
          UpdateEnabledComponents::greySelectButtonsDependingOnSelection);
      Thread.sleep(2000);

      // Select all links, as the UI's "select links" button does.
      SwingUtilities.invokeAndWait(() -> {
        ContextMananger.getLinkManager().selectAll();
        FrEnd.updateGUIToReflectSelectionChange();
      });

      // Wait for render frames to process the pending GUI update.
      final long deadline = System.currentTimeMillis() + 15000;
      int children = 0;
      while (System.currentTimeMillis() < deadline) {
        children = scalarsChildCount();
        if (children == 4) {
          break;
        }
        Thread.sleep(500);
      }

      // Radius, Length, Elasticity, Damping.
      assertEquals(4, children,
          "scalars panel should repopulate after selecting links");
    } finally {
      for (final Frame frame : Frame.getFrames()) {
        frame.dispose();
      }
    }
  }
}
