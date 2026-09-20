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
import com.springie.messages.commands.WheelDemoMessage;
import com.springie.render.SetUpCode;

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
  void demosAppearGroupedAtEndOfLeafDropdown() {
    final Choice leaf = FrEnd.choose_initial.choice;
    final int n = leaf.getItemCount();
    assertTrue(n > DemoCatalog.DEMOS.length,
        "the leaf dropdown must offer files as well as demos");
    for (int i = 0; i < DemoCatalog.DEMOS.length; i++) {
      assertEquals(DemoCatalog.DEMOS[i].label(),
          leaf.getItem(n - DemoCatalog.DEMOS.length + i),
          "the demos must sit grouped at the end of the file list");
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
    // The file card must launch each demo through the exact message the
    // Models > Demos menu enqueues -- same class, same builder, same
    // universe setup.
    assertTrue(
        DemoCatalog.forLabel("Demo: Sidewinder").newMessage() instanceof SidewinderDemoMessage);
    assertTrue(
        DemoCatalog.forLabel("Demo: Crawler").newMessage() instanceof CrawlerDemoMessage);
    assertTrue(DemoCatalog.forLabel("Demo: Spider Tank")
        .newMessage() instanceof SpiderTankDemoMessage);
    assertTrue(
        DemoCatalog.forLabel("Demo: Wheel").newMessage() instanceof WheelDemoMessage);
    assertNull(DemoCatalog.forLabel("Moscow"),
        "a model file must not dispatch to any demo");
  }

  @Test
  void selectDemoShowsItInTheDropdown() throws Exception {
    try {
      SwingUtilities.invokeAndWait(() -> panel().selectDemo("Wheel"));
      assertEquals("Demo: Wheel",
          FrEnd.choose_initial.choice.getSelectedItem(),
          "choosing the demo from the menu must show it in the file card");
    } finally {
      SwingUtilities.invokeAndWait(() -> FrEnd.choose_initial.choice.select(0));
    }
  }

  /**
   * Selecting each demo entry in the file card and pressing the launch
   * button must build the same model the menu launch produces: the demo
   * builders' node/link counts.
   */
  @Test
  void launchButtonLaunchesEachSelectedDemo() throws Exception {
    final String[] labels = {
        "Demo: Sidewinder", "Demo: Crawler", "Demo: Spider Tank", "Demo: Wheel", "Demo: Slinky"};
    // Node counts the demo builders produce (their own tests pin these).
    final int[] nodes = {
        SidewinderDemo.SEGMENTS + 3, 14, 16, 17, 27};
    final int[] links = {
        3 * SidewinderDemo.SEGMENTS + 3, -1, -1, 56, 92};

    try {
      for (int d = 0; d < labels.length; d++) {
        final String label = labels[d];
        SwingUtilities.invokeAndWait(() -> {
          final Choice leaf = FrEnd.choose_initial.choice;
          leaf.select(label);
          leaf.dispatchEvent(new ItemEvent(leaf, ItemEvent.ITEM_STATE_CHANGED,
              label, ItemEvent.SELECTED));
          // The rightmost button's action: a demo entry launches the demo,
          // a file entry restarts from the file.
          panel().launchSelected();
        });

        // The demo's launch message runs on the animation thread; wait for
        // the build to land.
        final long deadline = System.currentTimeMillis() + 60000;
        int got_nodes = -1;
        int got_links = -1;
        while (System.currentTimeMillis() < deadline) {
          got_nodes = ContextManager.getNodeManager().element.size();
          got_links =
              ContextManager.getNodeManager().getLinkManager().element.size();
          // Wait for the full build: nodes land before links, so checking
          // nodes alone can catch the build mid-flight (links still being
          // added). Only break when both counts match (when the demo has
          // an expected link count).
          if (got_nodes == nodes[d]
              && (links[d] < 0 || got_links == links[d])) {
            break;
          }
          Thread.sleep(25);
        }
        assertEquals(nodes[d], got_nodes,
            label + " must build its demo's node count via the launch button");
        if (links[d] >= 0) {
          assertEquals(links[d], got_links,
              label + " must build its demo's link count via the launch button");
        } else {
          assertTrue(got_links > got_nodes,
              label + " must build a linked structure via the launch button");
        }
      }
    } finally {
      // Leave the file card on a model file, as the other tests expect.
      SwingUtilities.invokeAndWait(() -> FrEnd.choose_initial.choice.select(0));
    }
  }

  /**
   * The boot path reads the same dropdown: with a demo selected, starting
   * (or restarting) the model must build the demo, not blow up on the
   * missing file path.
   */
  @Test
  void initialLoadBuildsSelectedDemo() throws Exception {
    try {
      SwingUtilities.invokeAndWait(
          () -> FrEnd.choose_initial.choice.select("Demo: Wheel"));
      // The same lock the message pump holds while running model-building
      // messages: the animation thread must not step physics mid-build.
      synchronized (ContextManager.class) {
        SetUpCode.clearAndThenAddInitialObjects();
      }
      assertEquals(17,
          ContextManager.getNodeManager().element.size(),
          "the initial load must build the selected demo");
      assertEquals(56,
          ContextManager.getNodeManager().getLinkManager().element.size(),
          "the initial load must build the selected demo's links");
    } finally {
      // Leave a model file loaded, as the other tests expect.
      SwingUtilities.invokeAndWait(() -> FrEnd.choose_initial.choice.select(0));
      synchronized (ContextManager.class) {
        SetUpCode.clearAndThenAddInitialObjects();
      }
    }
  }
}
