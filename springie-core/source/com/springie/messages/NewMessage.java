//This program has been placed into the public domain by its author.
package com.springie.messages;

public abstract class NewMessage {
  public Object context;

  public NewMessage(Object context) {
    this.context = context;
  }

  public abstract Object execute();
}
