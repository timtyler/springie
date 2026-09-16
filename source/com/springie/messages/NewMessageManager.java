//This program has been placed into the public domain by its author.
package com.springie.messages;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NewMessageManager {
  private static final Logger logger = LoggerFactory.getLogger(NewMessageManager.class);

  // Guarded by the NewMessageManager instance: add() is called from the
  // AWT event thread (menu clicks), process() from the animation thread.
  final ArrayList<NewMessage> messages = new ArrayList<>();

  public final void add(NewMessage msg) {
    synchronized (this.messages) {
      this.messages.add(msg);
    }
  }

  public final int size() {
    synchronized (this.messages) {
      return this.messages.size();
    }
  }

  public final void process() {
    final NewMessage[] batch;
    synchronized (this.messages) {
      batch = this.messages.toArray(new NewMessage[0]);
      this.messages.clear();
    }

    for (int n = 0; n < batch.length; n++) {
      try {
        batch[n].execute();
      } catch (RuntimeException e) {
        logger.debug("Error processing message (number " + n + "):");
        logger.error("Unexpected exception", e);
      }
    }
  }
}
