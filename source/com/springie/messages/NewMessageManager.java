//This program has been placed into the public domain by its author.
package com.springie.messages;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NewMessageManager {
  private static final Logger logger = LoggerFactory.getLogger(NewMessageManager.class);

  ArrayList<NewMessage> messages = new ArrayList<>();

  public final void add(NewMessage msg) {
    this.messages.add(msg);
  }

  public final void process() {
    final int number_of_messages = this.messages.size();

    for (int n = 0; n < number_of_messages; n++) {
      try {
        final NewMessage msg = this.messages.get(n);
        msg.execute();
      } catch (RuntimeException e) {
        logger.debug("Error processing message (number " + n + "):");
        logger.error("Unexpected exception", e);
      }
    }

    for (int n = number_of_messages; --n >= 0;) {
      this.messages.remove(n);
    }
  }
}
