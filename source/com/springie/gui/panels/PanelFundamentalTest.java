// This program has been placed into the public domain by its author.

package com.springie.gui.panels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Button;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Label;
import java.awt.event.ItemEvent;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.demos.DemoCatalog;
import com.springie.demos.SidewinderDemo;
import com.springie.gui.GuiTestSupport;
import com.springie.gui.components.ImageButton;
import com.springie.messages.commands.CrawlerDemoMessage;
import com.springie.messages.commands.SidewinderDemoMessage;
import com.springie.messages.commands.SpiderTankDemoMessage;
import com.springie.messages.commands.HamsterWheelDemoMessage;

/**
 * Tests for the floppy-disc toggle in the bottom button bar: pressed in
 * shows the two preset dropdowns (the default); pressed out swaps them for
 * the current filename plus Load and Save buttons, with the launch button
 * visible in both cases.
 */
public class PanelFundamentalTest {

  @BeforeAll
  static void boot() throws Exception {
    GuiTestSupport.bootApp();
  }

  @BeforeEach
  void setUp() throws Exception {
    // FrEnd.panel_fundamental is a static singleton shared by every test
    // here, and ImageButton.setState is a no-op when the state is unchanged,
    // so force the default (pressed in, presets card) before each test.
    final PanelFundamental p = panel();
    p.button_file_presets.setState(true);
    p.showPresetsCard(true);
  }

  @AfterAll
  static void dispose() throws Exception {
    GuiTestSupport.disposeFrames();
  }

  private PanelFundamental panel() {
    return FrEnd.panel_fundamental;
  }

  private ImageButton findFloppyButton() {
    return panel().button_file_presets;
  }

  @Test
  void floppyButtonDefaultsToPressedShowingPresets() {
    final ImageButton floppy = findFloppyButton();
    assertNotNull(floppy, "expected the floppy toggle button in the bar");
    assertTrue(floppy.getRadio(), "the floppy button must be a toggle");
    assertTrue(floppy.getState(), "pressed in (presets) is the default");
    assertTrue(panel().card_presets.isVisible(), "presets card visible by default");
    assertFalse(panel().card_file.isVisible(), "file card hidden by default");
  }

  @Test
  void togglingOutShowsFileCardWithCurrentFilename() {
    FrEnd.setFilePath("file:///tmp/some/dir/mymodel.spr");

    final ImageButton floppy = findFloppyButton();
    floppy.setState(false); // pressed out -> file card

    assertFalse(panel().card_presets.isVisible());
    assertTrue(panel().card_file.isVisible());

    boolean sawLabel = false;
    boolean sawLoad = false;
    boolean sawSave = false;
    for (final Component c : panel().card_file.getComponents()) {
      if (c instanceof Label) {
        assertEquals("mymodel.spr", ((Label) c).getText());
        sawLabel = true;
      }
      if (c instanceof Button) {
        final String label = ((Button) c).getLabel();
        sawLoad |= "Load".equals(label);
        sawSave |= "Save".equals(label);
      }
    }
    assertTrue(sawLabel, "file card must show the current filename");
    assertTrue(sawLoad, "file card must have a Load button");
    assertTrue(sawSave, "file card must have a Save button");

    floppy.setState(true); // back in -> presets card
    assertTrue(panel().card_presets.isVisible());
    assertFalse(panel().card_file.isVisible());
  }

  @Test
  void filenameReflectsLaterLoads() {
    final ImageButton floppy = findFloppyButton();
    floppy.setState(false);

    FrEnd.setFilePath("file://C:\\models\\icosahedron.spr");
    panel().reflectFileName();

    for (final Component c : panel().card_file.getComponents()) {
      if (c instanceof Label) {
        assertEquals("icosahedron.spr", ((Label) c).getText());
      }
    }
  }

  @Test
  void launchButtonStaysVisibleInBothCards() {
    assertNotNull(FrEnd.button_restart, "launch button must exist");
    assertTrue(FrEnd.button_restart.isVisible());
    assertTrue(FrEnd.button_restart.isEnabled());

    findFloppyButton().setState(false);
    assertTrue(FrEnd.button_restart.isVisible());
    assertTrue(FrEnd.button_restart.isEnabled());

    findFloppyButton().setState(true);
    assertTrue(FrEnd.button_restart.isVisible());
    assertTrue(FrEnd.button_restart.isEnabled());
  }

  @Test
  void saveWritesStraightBackToLocalFile() throws Exception {
    final Path dir = Files.createTempDirectory("springie-floppy-test");
    final Path file = dir.resolve("quick.spr");
    FrEnd.setFilePath("file://" + file.toString());

    panel().chooseSaveFile();

    assertTrue(Files.exists(file), "save must write the current file");
    assertTrue(Files.size(file) > 0, "saved file must have content");
  }

  @Test
  void presetDropdownsSurviveToggleRoundTripAfterRepopulation()
      throws Exception {
    final PanelFundamental p = panel();
    final Choice index = FrEnd.choose_preset_index.choice;
    final Choice leaf = FrEnd.choose_initial.choice;

    // Pick the Hexagonal index, as a user would: repopulating the leaf
    // dropdown adds much longer item names (Choice.select fires no event,
    // so dispatch the selection the way a user click would).
    SwingUtilities.invokeAndWait(() -> {
      index.select("Hexagonal");
      index.dispatchEvent(new ItemEvent(index, ItemEvent.ITEM_STATE_CHANGED,
          "Hexagonal", ItemEvent.SELECTED));
    });
    final int leafItems = leaf.getItemCount();
    assertTrue(leafItems > 4, "the hexagonal index should offer many models");
    final int leafWidth = leaf.getSize().width;
    assertTrue(leafWidth > 0, "the leaf dropdown must be laid out");

    try {
      // Toggle out to the file card and back.
      final ImageButton floppy = findFloppyButton();
      SwingUtilities.invokeAndWait(() -> {
        floppy.setState(false);
        p.showPresetsCard(false);
      });
      SwingUtilities.invokeAndWait(() -> {
        floppy.setState(true);
        p.showPresetsCard(true);
      });

      // Both dropdowns must still be there, fully populated, and the leaf
      // dropdown must not have grown: a wider dropdown wraps onto a clipped
      // second row of the button bar's FlowLayout and looks like it vanished.
      assertTrue(index.isShowing() && leaf.isShowing(),
          "both preset dropdowns must still be visible");
      assertEquals(leafItems, leaf.getItemCount(),
          "toggling must not lose the repopulated models");
      assertEquals(leafWidth, leaf.getSize().width,
          "the leaf dropdown must keep its width after a toggle round trip");
    } finally {
      // Leave the shared statics as the other tests expect them.
      SwingUtilities.invokeAndWait(() -> {
        index.select("Presets");
        index.dispatchEvent(new ItemEvent(index, ItemEvent.ITEM_STATE_CHANGED,
            "Presets", ItemEvent.SELECTED));
      });
    }
  }

  @Test
  void asm32aSitsJustUnderMoscowInTheDefaultPresets() throws Exception {
    final Choice leaf = FrEnd.choose_initial.choice;
    assertTrue(leaf.getItemCount() > 1, "the default preset index must offer models");
    assertEquals("Moscow", leaf.getItem(0),
        "Moscow must stay the first default preset");
    assertEquals("ASM (32)a", leaf.getItem(1),
        "ASM (32)a must sit just under Moscow in the default presets");
  }

  @Test
  void cubeIsOfferedInTheDefaultPresets() throws Exception {
    final Choice leaf = FrEnd.choose_initial.choice;
    boolean found = false;
    for (int i = 0; i < leaf.getItemCount(); i++) {
      if ("Cube".equals(leaf.getItem(i))) {
        found = true;
      }
    }
    assertTrue(found, "Cube must be offered in the default presets");
  }

  @Test
  void leafDropdownOffersFilesOnly() {
    final Choice leaf = FrEnd.choose_initial.choice;
    assertTrue(leaf.getItemCount() > 0,
        "the leaf dropdown must offer model files");
    for (int i = 0; i < leaf.getItemCount(); i++) {
      assertNull(DemoCatalog.forName(leaf.getItem(i)),
          "the leaf dropdown must not mix demos into the file list");
    }
  }

  @Test
  void demosDropdownOffersTheCataloguedDemos() {
    final Choice demos = panel().choose_demo.choice;
    assertEquals(DemoCatalog.DEMOS.length + 1, demos.getItemCount(),
        "the demos dropdown must offer the catalogued demos plus a placeholder");
    assertEquals(PanelFundamental.DEMO_PLACEHOLDER, demos.getItem(0),
        "the demos dropdown must open on its placeholder");
    for (int i = 0; i < DemoCatalog.DEMOS.length; i++) {
      assertEquals(DemoCatalog.DEMOS[i].name, demos.getItem(i + 1),
          "demos dropdown item " + i + " must match the catalog");
    }
  }

  @Test
  void demosMenuOffersTheSameDemosAsTheCatalog() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      java.awt.Menu models = null;
      final java.awt.MenuBar bar = FrEnd.frame_main.getMenuBar();
      for (int i = 0; i < bar.getMenuCount(); i++) {
        if ("Models".equals(bar.getMenu(i).getLabel())) {
          models = bar.getMenu(i);
        }
      }
      assertNotNull(models, "Models menu");
      java.awt.Menu demos = null;
      for (int i = 0; i < models.getItemCount(); i++) {
        final java.awt.MenuItem item = models.getItem(i);
        if (item instanceof java.awt.Menu && "Demos".equals(item.getLabel())) {
          demos = (java.awt.Menu) item;
        }
      }
      assertNotNull(demos, "Demos submenu");
      assertEquals(DemoCatalog.DEMOS.length, demos.getItemCount(),
          "the Demos menu must offer exactly the catalogued demos");
      for (int i = 0; i < DemoCatalog.DEMOS.length; i++) {
        assertEquals(DemoCatalog.DEMOS[i].name,
            demos.getItem(i).getLabel(),
            "Demos menu item " + i + " must match the catalog");
      }
    });
  }

  @Test
  void demoDispatchUsesTheDemosOwnLaunchMessages() {
    // The demos dropdown must launch each demo through the exact message
    // the Models > Demos menu enqueues -- same class, same builder, same
    // universe setup.
    assertTrue(
        DemoCatalog.forName("Sidewinder").newMessage() instanceof SidewinderDemoMessage);
    assertTrue(
        DemoCatalog.forName("Crawler").newMessage() instanceof CrawlerDemoMessage);
    assertTrue(DemoCatalog.forName("Spider Tank")
        .newMessage() instanceof SpiderTankDemoMessage);
    assertTrue(
        DemoCatalog.forName("Hamster wheel").newMessage() instanceof HamsterWheelDemoMessage);
    assertNull(DemoCatalog.forName("Moscow"),
        "a model file must not dispatch to any demo");
    assertNull(DemoCatalog.forName(PanelFundamental.DEMO_PLACEHOLDER),
        "the placeholder must not dispatch to any demo");
  }

  @Test
  void selectDemoShowsItInTheDropdown() throws Exception {
    try {
      SwingUtilities.invokeAndWait(() -> panel().selectDemo("Hamster wheel"));
      assertEquals("Hamster wheel",
          panel().choose_demo.choice.getSelectedItem(),
          "choosing the demo from the menu must show it in the demos dropdown");
    } finally {
      SwingUtilities.invokeAndWait(() -> panel().choose_demo.choice.select(0));
    }
  }

  /**
   * Choosing a demo in the file card's demos dropdown launches it at once
   * (like the Models > Demos menu): the demo builders' node/link counts.
   */
  @Test
  void choosingADemoLaunchesItAtOnce() throws Exception {
    final String[] names = {
        "Sidewinder", "Crawler", "Spider Tank", "Hamster wheel", "Slinky"};
    // Node counts the demo builders produce (their own tests pin these).
    // The crawler rebuild is still open, so its count may move again.
    final int[] nodes = {
        SidewinderDemo.SEGMENTS + 3, 13, 16, 14, 27};
    final int[] links = {
        3 * SidewinderDemo.SEGMENTS + 3, -1, -1, 43, 92};

    try {
      for (int d = 0; d < names.length; d++) {
        final String name = names[d];
        SwingUtilities.invokeAndWait(() -> {
          final Choice demos = panel().choose_demo.choice;
          demos.select(name);
          demos.dispatchEvent(new ItemEvent(demos, ItemEvent.ITEM_STATE_CHANGED,
              name, ItemEvent.SELECTED));
        });

        waitForBuild(nodes[d], links[d]);
        final int got_nodes = ContextManager.getNodeManager().element.size();
        final int got_links =
            ContextManager.getNodeManager().getLinkManager().element.size();
        assertEquals(nodes[d], got_nodes,
            name + " must build its demo's node count when chosen");
        if (links[d] >= 0) {
          assertEquals(links[d], got_links,
              name + " must build its demo's link count when chosen");
        } else {
          assertTrue(got_links > got_nodes,
              name + " must build a linked structure when chosen");
        }
      }
    } finally {
      // Leave the file card on a model file, as the other tests expect.
      SwingUtilities.invokeAndWait(() -> {
        panel().choose_demo.choice.select(0);
        FrEnd.choose_initial.choice.select(0);
      });
    }
  }

  /**
   * The launch button after a demo: with a demo chosen in the demos
   * dropdown, the launch button must rebuild the demo rather than the
   * selected model file.
   */
  @Test
  void launchButtonRebuildsTheChosenDemo() throws Exception {
    try {
      SwingUtilities.invokeAndWait(() -> {
        final Choice demos = panel().choose_demo.choice;
        demos.select("Hamster wheel");
        demos.dispatchEvent(new ItemEvent(demos, ItemEvent.ITEM_STATE_CHANGED,
            "Hamster wheel", ItemEvent.SELECTED));
      });
      waitForBuild(14, 43);
      final Object first_node_before =
          ContextManager.getNodeManager().element.get(0);

      SwingUtilities.invokeAndWait(() -> panel().launchSelected());

      // The node/link counts are the same before and after, so prove the
      // rebuild ran by waiting for fresh node instances to land.
      final long deadline = System.currentTimeMillis() + 60000;
      boolean rebuilt = false;
      while (System.currentTimeMillis() < deadline) {
        final java.util.List<?> elements =
            ContextManager.getNodeManager().element;
        if (!elements.isEmpty() && elements.get(0) != first_node_before) {
          rebuilt = true;
          break;
        }
        Thread.sleep(25);
      }
      assertTrue(rebuilt,
          "the launch button must rebuild the chosen demo");
      assertEquals(14,
          ContextManager.getNodeManager().element.size(),
          "the rebuilt demo must have the wheel's node count");
      assertEquals(43,
          ContextManager.getNodeManager().getLinkManager().element.size(),
          "the rebuilt demo must have the wheel's link count");
    } finally {
      SwingUtilities.invokeAndWait(() -> {
        panel().choose_demo.choice.select(0);
        FrEnd.choose_initial.choice.select(0);
      });
    }
  }

  /**
   * Waits for the animation thread to land a build with the given node
   * count (and link count, when non-negative).
   */
  private void waitForBuild(int nodes, int links) throws Exception {
    // The demo's launch message runs on the animation thread; wait for
    // the build to land.
    final long deadline = System.currentTimeMillis() + 60000;
    while (System.currentTimeMillis() < deadline) {
      final int got_nodes = ContextManager.getNodeManager().element.size();
      final int got_links =
          ContextManager.getNodeManager().getLinkManager().element.size();
      // Wait for the full build: nodes land before links, so checking
      // nodes alone can catch the build mid-flight (links still being
      // added). Only break when both counts match (when the demo has
      // an expected link count).
      if (got_nodes == nodes && (links < 0 || got_links == links)) {
        return;
      }
      Thread.sleep(25);
    }
  }
}
