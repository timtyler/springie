package com.springie.gui.panels.controls;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.gui.GuiTestSupport;

/**
 * Hitting a select button must not flip the Select: checkboxes back on.
 * The buttons end in FrEnd.reflectElementNumbersInGUI(), which rebuilds
 * the checkbox row; the rebuild has to preserve the user's states.
 */
class PanelControlsSelectMainTest {

  @BeforeAll
  static void boot() throws Exception {
    GuiTestSupport.bootApp();
  }

  @AfterAll
  static void dispose() throws Exception {
    GuiTestSupport.disposeFrames();
  }

  @Test
  void resetKeepsUncheckedBoxesUnchecked() throws Exception {
    final PanelControlsSelectMain panel = FrEnd.panel_edit_select_main;

    SwingUtilities.invokeAndWait(() -> {
      panel.resetPanelTypeSelector(true, true, true);
      panel.checkbox_select_links.setState(false);
      panel.checkbox_select_faces.setState(false);
    });

    // This is what the select buttons trigger (via FrEnd.postCleanup()).
    SwingUtilities.invokeAndWait(
        FrEnd::reflectAllValuesInGUIAfterSeriousEditing);

    assertTrue(panel.checkbox_select_nodes.getState(),
        "the nodes checkbox was left checked");
    assertFalse(panel.checkbox_select_links.getState(),
        "the links checkbox must stay unchecked after a select");
    assertFalse(panel.checkbox_select_faces.getState(),
        "the faces checkbox must stay unchecked after a select");
  }

  @Test
  void absentTypesStayAbsent() throws Exception {
    final PanelControlsSelectMain panel = FrEnd.panel_edit_select_main;

    SwingUtilities.invokeAndWait(
        () -> panel.resetPanelTypeSelector(true, false, false));

    // Links and faces keep their last checkbox object (readers never see
    // null), but the detached boxes are off the panel.
    assertTrue(panel.checkbox_select_links.getParent() == null,
        "a type with no elements must not show its checkbox");
    assertTrue(panel.checkbox_select_nodes.getParent() != null,
        "a type with elements must show its checkbox");

    // Restore the full row for the other tests sharing this panel.
    SwingUtilities.invokeAndWait(
        () -> panel.resetPanelTypeSelector(true, true, true));
  }
}
