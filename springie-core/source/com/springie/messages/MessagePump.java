//This program has been placed into the public domain by its author.
package com.springie.messages;

import com.springie.FrEnd;

/**
 * Drains the message queue once per frame. This replaces the old
 * MessageManager.process(): the per-frame generation bump lives here now
 * that every handler is a NewMessage.
 *
 * <p>Note: unlike the first version of this pump, it does NOT synthesize a
 * mouse-click message while the button is held down. The old code only
 * enqueued that click when the deprecated queue had traffic (effectively
 * never during pure mouse interaction); emitting it every frame re-ran
 * processMouseClick with stale coordinates, which froze the drag-box end
 * point and re-dragged a clicked node to the wrong place each frame.
 * The genuine AWT mouseClicked event still enqueues its own click message
 * via MainCanvas.
 */
public final class MessagePump {
  private MessagePump() {
    // static only
  }

  public static void processAll() {
    // Like the old MessageManager.process(), the generation bump happens
    // only when there is actually work to drain.
    if (FrEnd.new_message_manager.size() > 0) {
      FrEnd.generation++;

      FrEnd.new_message_manager.process();
    }
  }
}
