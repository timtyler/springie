//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.demos.SnakeDemo;
import com.springie.messages.NewMessage;

public class SnakeDemoMessage extends NewMessage {
  public SnakeDemoMessage() {
    super(null);
  }

  public Object execute() {
    SnakeDemo.build();
    return null;
  }
}
