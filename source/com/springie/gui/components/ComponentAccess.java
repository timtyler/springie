// This program has been placed into the public domain by its author.

package com.springie.gui.components;

import java.awt.Component;

public final class ComponentAccess {
  private ComponentAccess() {
    // ...
  }

  public static void setAccess(final Component c, final boolean v) {
    if (c.isEnabled() != v) {
      c.setEnabled(v);
    }
  }
}
