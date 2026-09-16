// Verifies the Models menu in the top menu bar.

package com.springie.gui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.CheckboxMenuItem;
import java.awt.GraphicsEnvironment;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.context.ModelManager;

class ModelsMenuTest {

  @BeforeEach
  void setUp() throws Exception {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    SwingUtilities.invokeAndWait(() -> FrEnd.main(new String[0]));
  }

  @AfterEach
  void tearDown() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      if (FrEnd.frame_main != null) {
        FrEnd.frame_main.dispose();
        FrEnd.frame_main = null;
      }
    });
  }

  /** A check that runs on the AWT thread. */
  @FunctionalInterface
  private interface EdtCheck {
    void run() throws Exception;
  }

  /**
   * Runs the check on the AWT thread, unwrapping failures so JUnit sees the
   * original AssertionError. ModelManager.notifyChanged() rebuilds this menu
   * via invokeLater (removeAll then re-add); reading it off the EDT can catch
   * it mid-rebuild and see a half-empty menu, so all reads go through here
   * to serialize with the rebuild.
   */
  private static void onEdt(EdtCheck check) throws Exception {
    final Throwable[] failure = new Throwable[1];
    SwingUtilities.invokeAndWait(() -> {
      try {
        check.run();
      } catch (Throwable t) {
        failure[0] = t;
      }
    });
    if (failure[0] instanceof AssertionError) {
      throw (AssertionError) failure[0];
    }
    if (failure[0] instanceof Exception) {
      throw (Exception) failure[0];
    }
    if (failure[0] instanceof Error) {
      throw (Error) failure[0];
    }
  }

  private Menu findModelsMenu() {
    final MenuBar bar = FrEnd.frame_main.getMenuBar();
    assertNotNull(bar, "menu bar");
    for (int i = 0; i < bar.getMenuCount(); i++) {
      final Menu menu = bar.getMenu(i);
      if ("Models".equals(menu.getLabel())) {
        return menu;
      }
    }
    return null;
  }

  @Test
  void modelsMenuListsLoadedModels() throws Exception {
    onEdt(() -> {
      final Menu models = findModelsMenu();
      assertNotNull(models, "Models menu");

      boolean has_close = false;
      boolean has_checkbox = false;
      for (int i = 0; i < models.getItemCount(); i++) {
        final MenuItem item = models.getItem(i);
        if (item == null) {
          continue; // separator
        }
        assertFalse("Load model...".equals(item.getLabel()),
            "no Load model... item");
        if (MenuBarTop.CLOSE_MODEL.equals(item.getLabel())) {
          has_close = true;
        } else if (item instanceof CheckboxMenuItem) {
          has_checkbox = true;
        }
      }

      assertTrue(has_close, "Close current model item");
      assertTrue(has_checkbox, "at least one model checkbox");
    });
  }

  @Test
  void modelsMenuHasPresetsSubmenu() throws Exception {
    onEdt(() -> {
      final Menu models = findModelsMenu();
      assertNotNull(models, "Models menu");

      Menu presets = null;
      for (int i = 0; i < models.getItemCount(); i++) {
        final MenuItem item = models.getItem(i);
        if (item instanceof Menu && "Presets".equals(item.getLabel())) {
          presets = (Menu) item;
        }
      }
      assertNotNull(presets, "Presets submenu");

      int indexes = 0;
      int leaves = 0;
      for (int i = 0; i < presets.getItemCount(); i++) {
        final MenuItem index = presets.getItem(i);
        if (index instanceof Menu) {
          indexes++;
          leaves += ((Menu) index).getItemCount();
        }
      }
      assertTrue(indexes > 0, "at least one preset index, got " + indexes);
      assertTrue(leaves > 0, "at least one preset model, got " + leaves);
    });
  }

  @Test
  void choosingPresetFromMenuUpdatesBottomBarDropdowns() throws Exception {
    final String[] captured = new String[3];
    final ActionListener[] captured_listener = new ActionListener[1];
    final MenuItem[] captured_leaf = new MenuItem[1];
    onEdt(() -> {
      final Menu models = findModelsMenu();
      assertNotNull(models, "Models menu");

      Menu presets = null;
      for (int i = 0; i < models.getItemCount(); i++) {
        final MenuItem item = models.getItem(i);
        if (item instanceof Menu && "Presets".equals(item.getLabel())) {
          presets = (Menu) item;
        }
      }
      assertNotNull(presets, "Presets submenu");

      final Menu index_menu = (Menu) presets.getItem(0);
      final MenuItem leaf = index_menu.getItem(0);
      final ActionListener[] listeners = leaf.getActionListeners();
      assertTrue(listeners.length > 0, "leaf has an action");

      captured[0] = index_menu.getLabel();
      captured[1] = leaf.getLabel();
      captured_listener[0] = listeners[0];
      captured_leaf[0] = leaf;
    });

    SwingUtilities.invokeAndWait(() -> captured_listener[0].actionPerformed(
        new ActionEvent(captured_leaf[0], ActionEvent.ACTION_PERFORMED,
            captured[1])));

    assertEquals(captured[0],
        FrEnd.choose_preset_index.choice.getSelectedItem(),
        "index dropdown follows the menu");
    assertEquals(captured[1],
        FrEnd.choose_initial.choice.getSelectedItem(),
        "leaf dropdown follows the menu");
    assertEquals(FrEnd.choose_initial.hashtable.get(captured[1]),
        FrEnd.next_file_path, "next_file_path follows the menu");
  }

  @Test
  void modelsMenuReflectsNewlyLoadedModel() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> ModelManager.loadNewModel("resource://models/cube.spr"));
    // let the async menu rebuild run
    SwingUtilities.invokeAndWait(() -> {
    });

    onEdt(() -> {
      final Menu models = findModelsMenu();
      assertNotNull(models, "Models menu");

      int checkboxes = 0;
      for (int i = 0; i < models.getItemCount(); i++) {
        if (models.getItem(i) instanceof CheckboxMenuItem) {
          checkboxes++;
        }
      }
      assertTrue(checkboxes >= 2, "two models listed, got " + checkboxes);
    });
  }
}
