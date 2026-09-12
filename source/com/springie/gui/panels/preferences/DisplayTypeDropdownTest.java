// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Choice;
import java.awt.Component;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.gui.GuiTestSupport;
import com.springie.render.RendererDelegator;
import com.springie.render.modules.modern.ModularRendererNew;
import com.springie.render.modules.original.ModularRendererOld;

/**
 * The Display type dropdown (the three renderer options) lives under the
 * Renderer tab now, not at the top of the Display panel. It appears twice:
 * once at the top of the shared Renderer tab (modern and ray-traced
 * renderers) and once in the original renderer's own Renderer tab, so that
 * picking the Original renderer never strands the user with no way back.
 * The two copies stay in sync.
 *
 * <p>Selections are driven by dispatching a synthetic ItemEvent to the
 * AWT Choice's listeners, which exercises the real listener path
 * (Choice.select() alone fires no event).
 */
class DisplayTypeDropdownTest {

  @BeforeAll
  static void boot() throws Exception {
    GuiTestSupport.bootApp();
  }

  /** Pin the Modern renderer before each test; also restores the state. */
  @BeforeEach
  void pinModernRenderer() throws Exception {
    selectRenderer(sharedDropdown(), "Modern");
  }

  /** The Display type row: a "Display type" label plus the dropdown. */
  private static Choice displayTypeChoice(Panel holder) throws Exception {
    final Choice[] found = new Choice[1];
    SwingUtilities.invokeAndWait(() -> {
      for (int i = 0; i < holder.getComponentCount() && found[0] == null; i++) {
        found[0] = findChoice(holder.getComponent(i));
      }
    });
    return found[0];
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

  private static Choice sharedDropdown() throws Exception {
    return displayTypeChoice(FrEnd.panel_preferences_shared_show.panel);
  }

  private static Choice originalTabDropdown() throws Exception {
    return displayTypeChoice(
        FrEnd.panel_preferences_renderer_original.panel_renderer_tab);
  }

  private static boolean hasDisplayTypeLabel(Panel holder) throws Exception {
    final boolean[] found = new boolean[1];
    SwingUtilities.invokeAndWait(() -> {
      found[0] = findLabel(holder, "Display type");
    });
    return found[0];
  }

  private static boolean findLabel(Component component, String text) {
    if (component instanceof Label) {
      return text.equals(((Label) component).getText());
    }
    if (component instanceof Panel) {
      final Panel panel = (Panel) component;
      for (int i = 0; i < panel.getComponentCount(); i++) {
        if (findLabel(panel.getComponent(i), text)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Drives the real item listener the way a user picking from the
   * dropdown would.
   */
  private static void selectRenderer(Choice dropdown, String namePart)
      throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      String target = null;
      for (int i = 0; i < dropdown.getItemCount(); i++) {
        if (dropdown.getItem(i).contains(namePart)) {
          target = dropdown.getItem(i);
        }
      }
      assertNotNull(target, "no renderer option containing '" + namePart + "'");
      final ItemEvent event = new ItemEvent(dropdown,
          ItemEvent.ITEM_STATE_CHANGED, target, ItemEvent.SELECTED);
      for (final ItemListener listener : dropdown.getItemListeners()) {
        listener.itemStateChanged(event);
      }
    });
  }

  private static String selectedItem(Choice dropdown) throws Exception {
    final String[] selected = new String[1];
    SwingUtilities.invokeAndWait(() -> {
      selected[0] = dropdown.getSelectedItem();
    });
    return selected[0];
  }

  @Test
  void rendererTabStartsWithDisplayType() throws Exception {
    final Panel shared = FrEnd.panel_preferences_shared_show.panel;
    final Choice dropdown = sharedDropdown();
    assertNotNull(dropdown, "the shared Renderer tab must hold a dropdown");
    assertTrue(hasDisplayTypeLabel(shared),
        "the shared Renderer tab must label it 'Display type'");
    assertEquals(3, dropdown.getItemCount(),
        "the dropdown must offer the three renderers");

    // It must be the first row of the tab, not buried below the options.
    final Component[] first = new Component[1];
    SwingUtilities.invokeAndWait(() -> {
      first[0] = shared.getComponent(0);
    });
    assertTrue(first[0] instanceof Panel
        && findChoice(first[0]) == dropdown,
        "the Display type row must sit at the top of the Renderer tab");
  }

  @Test
  void originalRendererTabHasItsOwnDropdown() throws Exception {
    final Choice dropdown = originalTabDropdown();
    assertNotNull(dropdown,
        "the original renderer's Renderer tab must hold a dropdown");
    assertEquals(3, dropdown.getItemCount(),
        "the original tab's dropdown must offer the three renderers");
  }

  @Test
  void switchingAwayAndBackNeverStrandsTheUser() throws Exception {
    final Choice shared = sharedDropdown();

    // Pick the Original renderer from the shared tab's dropdown.
    selectRenderer(shared, "Original");
    SwingUtilities.invokeAndWait(() -> {
      assertTrue(RendererDelegator.renderer instanceof ModularRendererOld,
          "picking Original must swap in the original renderer");
      assertSame(FrEnd.panel_preferences_renderer_original.panel,
          FrEnd.panel_preferences_display.panel_main.getComponent(0),
          "the Display panel must show the original renderer's tabs");
    });

    // The original tab's own dropdown stayed in sync and still works.
    final Choice originalTab = originalTabDropdown();
    assertEquals(selectedItem(shared), selectedItem(originalTab),
        "the two dropdowns must stay in sync");

    // Switch back from inside the original renderer's tab bar.
    selectRenderer(originalTab, "Modern");
    SwingUtilities.invokeAndWait(() -> {
      assertTrue(RendererDelegator.renderer instanceof ModularRendererNew,
          "picking Modern must swap the modern renderer back in");
      assertSame(FrEnd.panel_preferences_renderer_modern.panel,
          FrEnd.panel_preferences_display.panel_main.getComponent(0),
          "the Display panel must show the modern renderer's tabs again");
    });
    assertEquals(selectedItem(originalTab), selectedItem(shared),
        "the shared dropdown must follow the switch back");
  }
}
