// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import com.springie.context.ContextManager;
import com.springie.elements.DeepObjectColourCalculator;

/**
 * Depth fog, exactly as the default renderer applies it: distant objects
 * are darkened towards black. A no-op when no model is loaded, so headless
 * unit tests can shade without a NodeManager.
 */
final class Fog {
  private Fog() {
    // ...
  }

  static int applyFog(int colour, int z) {
    if (ContextManager.getNodeManager() == null) {
      return colour;
    }
    return DeepObjectColourCalculator.getColourOfDeepObject(colour, z);
  }
}
