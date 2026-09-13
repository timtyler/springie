//This program has been placed into the public domain by its author.
package com.springie.messages;

import java.awt.Point;

import com.springie.FrEnd;

/**
 * Drains the message queue once per frame. This replaces the old
 * MessageManager.process(): the per-frame generation bump and the
 * mouse-click enqueue live here now that every handler is a NewMessage.
 */
public final class MessagePump {
  private MessagePump() {
    // static only
  }

  public static void processAll() {
    if (FrEnd.mouse_pressed) {
      FrEnd.new_message_manager.add(new NewMessage(
          new Point(FrEnd.last_mousex, FrEnd.last_mousey)) {
        public Object execute() {
          final Point p = (Point) this.context;
          FrEnd.springieMouseClicked(p.x, p.y);
          return null;
        }
      });
    }

    // Like the old MessageManager.process(), the generation bump happens
    // only when there is actually work to drain.
    if (FrEnd.new_message_manager.size() > 0) {
      FrEnd.generation++;

      FrEnd.new_message_manager.process();
    }
  }
}
