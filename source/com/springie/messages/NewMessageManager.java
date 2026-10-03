//This program has been placed into the public domain by its author.
package com.springie.messages;

import com.springie.context.ContextManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NewMessageManager {
  private static final Logger logger = LoggerFactory.getLogger(NewMessageManager.class);

  // Lock-free multi-producer queue: add() is called from the AWT event
  // thread (menu clicks), the JavaFX thread (viewer commands), and the
  // physics thread itself (deferred node create/delete); process() drains
  // from the animation thread. (Tim, 2026-10-01: modernised from a
  // hand-rolled synchronized ArrayList.)
  final ConcurrentLinkedQueue<NewMessage> messages = new ConcurrentLinkedQueue<>();

  public final void add(final NewMessage msg) {
    this.messages.add(msg);
  }

  public final int size() {
    return this.messages.size();
  }

  public final void process() {
    final List<NewMessage> batch = new ArrayList<>();
    NewMessage msg;
    while ((msg = this.messages.poll()) != null) {
      batch.add(msg);
    }

    for (int n = 0; n < batch.size(); n++) {
      try {
        // Mutually exclusive with rendering (see RendererDelegator):
        // a model-building message must not run concurrently with
        // the AWT-thread renderer accessing the model.
        synchronized (ContextManager.class) {
          batch.get(n).execute();
        }
      } catch (RuntimeException e) {
        logger.debug("Error processing message (number " + n + "):");
        logger.error("Unexpected exception", e);
      }
    }
  }
}
