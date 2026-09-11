// This program has been placed into the public domain by its author.

package com.springie.gui.panels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Button;
import java.awt.Component;
import java.awt.Label;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.gui.GuiTestSupport;
import com.springie.gui.components.ImageButton;

/**
 * Tests for the floppy-disc toggle in the bottom button bar: pressed in
 * shows the two preset dropdowns (the default); pressed out swaps them for
 * the current filename plus Load and Save buttons, with the launch button
 * visible in both cases.
 */
public class PanelFundamentalTest {

  @BeforeEach
  void setUp() throws Exception {
    GuiTestSupport.bootApp();
    // FrEnd.panel_fundamental is a static singleton shared by every test
    // here, and ImageButton.setState is a no-op when the state is unchanged,
    // so force the default (pressed in, presets card) before each test.
    final PanelFundamental p = panel();
    p.button_file_presets.setState(true);
    p.showPresetsCard(true);
  }

  @AfterEach
  void tearDown() throws Exception {
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
}
