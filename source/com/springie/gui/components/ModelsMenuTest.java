// Verifies the Models menu in the top menu bar.

package com.springie.gui.components;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.CheckboxMenuItem;
import java.awt.GraphicsEnvironment;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;

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
  void modelsMenuListsLoadedModels() {
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
  }

  @Test
  void modelsMenuHasPresetsSubmenu() {
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
  }

  @Test
  void modelsMenuReflectsNewlyLoadedModel() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> ModelManager.loadNewModel("resource://models/cube.spr"));
    // let the async menu rebuild run
    SwingUtilities.invokeAndWait(() -> {
    });

    final Menu models = findModelsMenu();
    assertNotNull(models, "Models menu");

    int checkboxes = 0;
    for (int i = 0; i < models.getItemCount(); i++) {
      if (models.getItem(i) instanceof CheckboxMenuItem) {
        checkboxes++;
      }
    }
    assertTrue(checkboxes >= 2, "two models listed, got " + checkboxes);
  }
}
