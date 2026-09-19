// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Checkbox;
import java.awt.Component;
import java.awt.Container;
import java.awt.ItemSelectable;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.gui.GUIStrings;
import com.springie.gui.GuiTestSupport;

/**
 * The dotted boundary-box overlay is a viewport aid, not part of the
 * universe: its checkbox must live on the Preferences Viewpoint tab and
 * nowhere on the Universe tab, and toggling it must flip
 * FrEnd.show_boundary_box.
 */
class BoundaryBoxCheckboxTest {

  @BeforeAll
  static void boot() throws Exception {
    GuiTestSupport.bootApp();
  }

  @AfterAll
  static void dispose() throws Exception {
    GuiTestSupport.disposeFrames();
  }

  private static Checkbox findBoundaryBoxCheckbox(Container root) {
    for (final Component c : root.getComponents()) {
      if (c instanceof Checkbox
          && GUIStrings.SHOW_BOUNDARY_BOX.equals(((Checkbox) c).getLabel())) {
        return (Checkbox) c;
      }
      if (c instanceof Container) {
        final Checkbox found = findBoundaryBoxCheckbox((Container) c);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static boolean contains(Container root, Component target) {
    for (final Component c : root.getComponents()) {
      if (c == target) {
        return true;
      }
      if (c instanceof Container && contains((Container) c, target)) {
        return true;
      }
    }
    return false;
  }

  private static void clickCheckbox(Checkbox checkbox, int state_change) {
    checkbox.setState(state_change == ItemEvent.SELECTED);
    final ItemEvent event = new ItemEvent((ItemSelectable) checkbox,
        ItemEvent.ITEM_STATE_CHANGED, checkbox, state_change);
    for (final ItemListener listener : checkbox.getItemListeners()) {
      listener.itemStateChanged(event);
    }
  }

  @Test
  void checkboxLivesOnViewpointTabNotUniverse() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      final Checkbox onViewpoint = findBoundaryBoxCheckbox(
          FrEnd.panel_preferences_viewpoint.panel);
      assertNotNull(onViewpoint,
          "the Show boundary box checkbox must be on the Viewpoint tab");
      assertTrue(
          contains(FrEnd.panel_preferences_viewpoint.panel, onViewpoint),
          "the checkbox must be reachable from the Viewpoint panel");
      assertNull(findBoundaryBoxCheckbox(FrEnd.panel_universe.panel),
          "the Show boundary box checkbox must not be on the Universe tab");
    });
  }

  @Test
  void checkboxTogglesTheFlag() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      final Checkbox checkbox =
          FrEnd.panel_preferences_viewpoint.checkbox_show_boundary_box;
      assertNotNull(checkbox, "expected the checkbox field to exist");
      clickCheckbox(checkbox, ItemEvent.SELECTED);
      assertTrue(FrEnd.show_boundary_box, "checking must turn the dots on");
      clickCheckbox(checkbox, ItemEvent.DESELECTED);
      assertFalse(FrEnd.show_boundary_box, "unchecking must turn the dots off");
    });
  }
}
