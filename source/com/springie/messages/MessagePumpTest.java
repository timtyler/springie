// This program has been placed into the public domain by its author.

package com.springie.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.gui.GuiTestSupport;

/**
 * While the mouse button is held down the pump must not synthesize its own
 * click message. (The old code only enqueued that click when the deprecated
 * queue had traffic -- effectively never during mouse interaction.) Emitting
 * it every frame re-ran processMouseClick with stale coordinates: the
 * drag-box end point froze at the stale position and a clicked node was
 * re-dragged to the wrong place on every frame, corrupting the model.
 */
class MessagePumpTest {

  @BeforeAll
  static void boot() throws Exception {
    GuiTestSupport.bootApp();
  }

  @Test
  void holdingTheMouseDownSynthesizesNoClick() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.new_message_manager.messages.clear();
      FrEnd.mouse_pressed = true;
      FrEnd.last_mousex = 12345;
      FrEnd.last_mousey = 67890;

      final int generation_before = FrEnd.generation;
      MessagePump.processAll();

      assertEquals(0, FrEnd.new_message_manager.size(),
          "the pump must not enqueue a click while the button is held");
      assertEquals(generation_before, FrEnd.generation,
          "no work was drained, so the generation must not bump");
      FrEnd.mouse_pressed = false;
    });
  }

  @Test
  void queuedWorkStillDrainsWhileTheMouseIsDown() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.new_message_manager.messages.clear();
      FrEnd.mouse_pressed = true;

      final boolean[] ran = {false};
      FrEnd.new_message_manager.add(new NewMessage(null) {
        @Override
        public Object execute() {
          ran[0] = true;
          return null;
        }
      });

      MessagePump.processAll();

      assertEquals(true, ran[0], "the queued message must still execute");
      assertEquals(0, FrEnd.new_message_manager.size(),
          "only the queued message may run -- no synthetic click");
      FrEnd.mouse_pressed = false;
    });
  }
}
