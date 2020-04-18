//This program has been placed into the public domain by its author.
package com.springie.messages;

import java.util.Vector;

import com.springie.utilities.log.Log;

public class NewMessageManager {
  Vector messages = new Vector();

  public final void add(NewMessage msg) {
    this.messages.addElement(msg);
  }

  public final void process() {
    final int number_of_messages = this.messages.size();

    for (int n = 0; n < number_of_messages; n++) {
      try {
        final NewMessage msg = (NewMessage) this.messages.elementAt(n);
        msg.execute();
      } catch (RuntimeException e) {
        Log.log("Error processing message (number " + n + "):");
        e.printStackTrace();
      }
    }

    for (int n = number_of_messages; --n >= 0;) {
      this.messages.removeElementAt(n);
    }
  }
}
