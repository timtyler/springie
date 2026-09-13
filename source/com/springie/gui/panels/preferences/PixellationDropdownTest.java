// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Choice;
import java.awt.Component;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.gui.GuiTestSupport;
import com.springie.render.RendererDelegator;

/**
 * The Pixellated dropdown (1x1 to 4x4 blocky display) sits in the shared
 * Renderer tab next to Anti-aliasing. Selections are driven by
 * dispatching a synthetic ItemEvent to the AWT Choice's listeners, which
 * exercises the real listener path (Choice.select() alone fires no
 * event).
 */
class PixellationDropdownTest {

  @BeforeAll
  static void boot() throws Exception {
    GuiTestSupport.bootApp();
  }

  @AfterEach
  void restoreOneByOne() throws Exception {
    fireChoice(pixellationChoice(), "1x1");
    assertEquals(1, RendererDelegator.pixellation);
  }

  /** The Pixellated row: a "Pixellated:" label plus the dropdown. */
  private static Choice pixellationChoice() throws Exception {
    final Choice[] found = new Choice[1];
    SwingUtilities.invokeAndWait(() -> {
      final Panel holder = FrEnd.panel_preferences_shared_show.panel;
      for (int i = 0; i < holder.getComponentCount() && found[0] == null;
          i++) {
        final Component row = holder.getComponent(i);
        if (row instanceof Panel && hasLabel((Panel) row, "Pixellated:")) {
          found[0] = findChoice(row);
        }
      }
    });
    assertNotNull(found[0], "Pixellated dropdown not found");
    return found[0];
  }

  private static boolean hasLabel(Panel panel, String text) {
    for (int i = 0; i < panel.getComponentCount(); i++) {
      final Component c = panel.getComponent(i);
      if (c instanceof Label && ((Label) c).getText().equals(text)) {
        return true;
      }
    }
    return false;
  }

  private static Choice findChoice(Component component) {
    if (component instanceof Choice) {
      return (Choice) component;
    }
    if (component instanceof Panel) {
      final Panel panel = (Panel) component;
      for (int i = 0; i < panel.getComponentCount(); i++) {
        final Choice choice = findChoice(panel.getComponent(i));
        if (choice != null) {
          return choice;
        }
      }
    }
    return null;
  }

  private static void fireChoice(Choice dropdown, String option)
      throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      final ItemEvent event = new ItemEvent(dropdown,
          ItemEvent.ITEM_STATE_CHANGED, option, ItemEvent.SELECTED);
      for (final ItemListener listener : dropdown.getItemListeners()) {
        listener.itemStateChanged(event);
      }
    });
  }

  @Test
  void offersOneToFour() throws Exception {
    final Choice choice = pixellationChoice();
    assertEquals(4, choice.getItemCount());
    assertEquals("1x1", choice.getItem(0));
    assertEquals("2x2", choice.getItem(1));
    assertEquals("3x3", choice.getItem(2));
    assertEquals("4x4", choice.getItem(3));
  }

  @Test
  void defaultsToOneByOne() throws Exception {
    assertEquals("1x1", pixellationChoice().getSelectedItem());
    assertEquals(1, RendererDelegator.pixellation);
  }

  @Test
  void selectingThreeByThreeSetsTheFactor() throws Exception {
    fireChoice(pixellationChoice(), "3x3");
    assertEquals(3, RendererDelegator.pixellation);
  }
}
