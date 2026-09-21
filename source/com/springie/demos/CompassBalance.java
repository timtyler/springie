// This code has been placed into the public domain by its author.

package com.springie.demos;

import java.util.Map;

/**
 * Enforces Tim's balance rule for a judged slate: the slate must carry
 * equal numbers heading N and S, and equal numbers heading E and W, so no
 * compass direction gains from any directional asymmetry in the engine.
 * "Judges ensure equal numbers of N and S -- and E and W."
 */
public final class CompassBalance {
  private CompassBalance() {
  }

  /**
   * Checks the balance rule over a slate mapping entry name to its
   * declared compass heading.
   *
   * @return a violation description, or null when the slate is balanced.
   */
  public static String checkBalanced(Map<String, CompassPoint> slate) {
    int n = 0;
    int s = 0;
    int e = 0;
    int w = 0;
    for (final CompassPoint heading : slate.values()) {
      switch (heading) {
        case N:
          n++;
          break;
        case S:
          s++;
          break;
        case E:
          e++;
          break;
        case W:
          w++;
          break;
        default:
          throw new AssertionError(heading);
      }
    }
    if (n != s) {
      return "unbalanced N/S: " + n + " heading N, " + s
          + " heading S (need equal numbers)";
    }
    if (e != w) {
      return "unbalanced E/W: " + e + " heading E, " + w
          + " heading W (need equal numbers)";
    }
    return null;
  }
}
