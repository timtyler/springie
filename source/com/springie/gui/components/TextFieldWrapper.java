// This program has been placed into the public domain by its author.

package com.springie.gui.components;

import java.awt.Color;
import java.awt.TextField;

public class TextFieldWrapper extends TextField {
  /**
   *
   */
  static final long serialVersionUID = -8600497821422579497L;

  public TextFieldWrapper() {
    super();
    setColours();
  }

  public TextFieldWrapper(int arg0) {
    super(arg0);
    setColours();
  }

  public TextFieldWrapper(String arg0, int arg1) {
    super(arg0, arg1);
    setColours();
  }

  public TextFieldWrapper(String arg0) {
    super(arg0);
    setColours();
  }

  private void setColours() {
    this.setBackground(Color.white);
    this.setForeground(Color.black);
  }
}
