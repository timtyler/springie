// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Checkbox;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.gui.GuiTestSupport;
import com.springie.render.RendererDelegator;

/**
 * Toggling the Grass/sky checkbox must repaint the background. The
 * background is baked into the cached bin tiles, so the damage-repair
 * pass (repaint_some_objects) is not enough -- a full repaint is needed.
 */
class ScenicBackgroundRepaintTest {

  @BeforeAll
  static void boot() throws Exception {
    GuiTestSupport.bootApp();
  }

  @AfterEach
  void restore() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      RendererDelegator.scenic_background = false;
      RendererDelegator.repaint_all_objects = false;
      RendererDelegator.repaint_some_objects = false;
      FrEnd.panel_preferences_shared_show.checkbox_scenic_background
          .setState(false);
    });
  }

  @AfterAll
  static void disposeFrames() throws Exception {
    GuiTestSupport.disposeFrames();
  }

  @Test
  void togglingScenicBackgroundRequestsFullRepaint() throws Exception {
    final boolean[] repaint_all = new boolean[1];
    final boolean[] scenic = new boolean[1];
    SwingUtilities.invokeAndWait(() -> {
      final Checkbox box =
          FrEnd.panel_preferences_shared_show.checkbox_scenic_background;
      // Start from a clean slate: no repaint requested.
      RendererDelegator.repaint_all_objects = false;
      RendererDelegator.repaint_some_objects = false;
      // Toggle the checkbox on. (setState() does not fire ItemEvents
      // programmatically in AWT, so dispatch directly to the listeners.)
      box.setState(true);
      final ItemEvent on = new ItemEvent(box, ItemEvent.ITEM_STATE_CHANGED,
          box.getLabel(), ItemEvent.SELECTED);
      for (final ItemListener l : box.getItemListeners()) {
        l.itemStateChanged(on);
      }
      repaint_all[0] = RendererDelegator.repaint_all_objects;
      scenic[0] = RendererDelegator.scenic_background;
    });
    assertTrue(scenic[0], "checkbox should set scenic_background");
    assertTrue(repaint_all[0],
        "toggling Grass/sky must request a full repaint (repaint_all_objects), "
            + "not just the damage-repair pass -- the background is baked "
            + "into the cached bin tiles");
  }

  @Test
  void togglingScenicBackgroundOffRequestsFullRepaint() throws Exception {
    final boolean[] repaint_all = new boolean[1];
    SwingUtilities.invokeAndWait(() -> {
      final Checkbox box =
          FrEnd.panel_preferences_shared_show.checkbox_scenic_background;
      // Start with it on, then toggle off.
      RendererDelegator.scenic_background = true;
      box.setState(true);
      RendererDelegator.repaint_all_objects = false;
      RendererDelegator.repaint_some_objects = false;
      box.setState(false);
      final ItemEvent off = new ItemEvent(box, ItemEvent.ITEM_STATE_CHANGED,
          box.getLabel(), ItemEvent.DESELECTED);
      for (final ItemListener l : box.getItemListeners()) {
        l.itemStateChanged(off);
      }
      repaint_all[0] = RendererDelegator.repaint_all_objects;
    });
    assertTrue(repaint_all[0],
        "unticking Grass/sky must also request a full repaint");
  }
}
