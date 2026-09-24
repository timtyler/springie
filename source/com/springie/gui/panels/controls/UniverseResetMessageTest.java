// This program has been placed into the public domain by its author.

package com.springie.gui.panels.controls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.awt.Checkbox;
import java.awt.ItemSelectable;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.gui.GuiTestSupport;
import com.springie.world.UniverseDefaults;

/**
 * The message-queue discipline behind the Universe tab's reset. Several
 * Universe checkboxes queue toggle messages when clicked, so the reset must
 * update them silently: a toggle queued by the reset itself would be
 * processed later and silently undo it.
 */
class UniverseResetMessageTest {

  @BeforeAll
  static void boot() throws Exception {
    GuiTestSupport.bootApp();
    // The snapshot is global: a test that loaded a model earlier in this
    // JVM would otherwise pollute the reset assertions below.
    UniverseDefaults.resetToFactoryDefaults();
  }

  @AfterEach
  void restoreDefaults() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.panel_universe.resetUniverse();
      drainMessageQueue();
    });
  }

  @AfterAll
  static void disposeFrames() throws Exception {
    GuiTestSupport.disposeFrames();
  }

  @Test
  void resetUniverseEnqueuesNoMessages() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      drainMessageQueue();

      // Scramble the toggle checkboxes with real clicks.
      final PanelControlsUniverse panel = FrEnd.panel_universe;
      clickCheckbox(panel.checkbox_continuously_centre_x, ItemEvent.SELECTED);
      clickCheckbox(panel.checkbox_collision_check, ItemEvent.DESELECTED);

      // Let the queued toggles take effect, then drain.
      FrEnd.new_message_manager.process();
      assertTrue(FrEnd.continuously_centre_x);
      assertFalse(FrEnd.check_collisions);
      drainMessageQueue();

      // The reset must restore the statics without queueing anything that
      // a later process() would turn back.
      panel.resetUniverse();
      assertEquals(0, FrEnd.new_message_manager.size(),
          "resetUniverse must not enqueue messages");

      FrEnd.new_message_manager.process();
      assertFalse(FrEnd.continuously_centre_x);
      assertTrue(FrEnd.check_collisions);
    });
  }

  @Test
  void clickDuringResetQueuesMessageThatUndoesIt() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      drainMessageQueue();

      // The hazard the silent update guards against: a click queues a
      // toggle message, the reset fixes the static directly, then the
      // queued toggle flips it back when processed.
      FrEnd.continuously_centre_x = true;
      clickCheckbox(FrEnd.panel_universe.checkbox_continuously_centre_x,
          ItemEvent.DESELECTED);
      FrEnd.continuously_centre_x = false;

      // The click queued a toggle message...
      assertEquals(1, FrEnd.new_message_manager.size());

      // ...which flips the static back when the queue is processed.
      FrEnd.new_message_manager.process();
      assertTrue(FrEnd.continuously_centre_x,
          "a queued toggle undoes the reset -- this is why the reset updates the checkboxes silently");

      // Restore the default for the next test.
      FrEnd.continuously_centre_x = false;
      drainMessageQueue();
    });
  }

  /**
   * Simulates a real user click: sets the visual state, then delivers the
   * ItemEvent the native peer would deliver. (AWT's programmatic setState
   * deliberately fires no listeners -- the event only comes from the peer
   * on a genuine click.)
   */
  private static void clickCheckbox(Checkbox checkbox, int state_change) {
    checkbox.setState(state_change == ItemEvent.SELECTED);
    final ItemEvent event = new ItemEvent((ItemSelectable) checkbox,
        ItemEvent.ITEM_STATE_CHANGED, checkbox, state_change);
    for (final ItemListener listener : checkbox.getItemListeners()) {
      listener.itemStateChanged(event);
    }
  }

  private static void drainMessageQueue() {
    while (FrEnd.new_message_manager.size() > 0) {
      FrEnd.new_message_manager.process();
    }
  }

}
