//This program has been placed into the public domain by its author.
package com.springie.utilities.general;

public abstract class Executor {
  public Object input;
  public Object output;

  public Executor() {
    //...
  }

  public Executor(Object input) {
    this.input = input;
  }

  public abstract Object execute(Object parameter);
}
