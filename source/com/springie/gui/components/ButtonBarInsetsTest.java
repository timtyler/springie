// This code has been placed into the public domain by its author

package com.springie.gui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Insets;

import org.junit.jupiter.api.Test;

/**
 * The button bar at the bottom of the main window carries two pixels of
 * breathing room at its top, separating the buttons from the window
 * above.
 */
public class ButtonBarInsetsTest {
  @Test
  public void buttonBarHasTwoPixelsOfTopInset() {
    final Insets insets = new ButtonBar().getInsets();
    assertEquals(2, insets.top, "top inset");
    assertEquals(0, insets.left, "left inset");
    assertEquals(0, insets.bottom, "bottom inset");
    assertEquals(0, insets.right, "right inset");
  }
}
