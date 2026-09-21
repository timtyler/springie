// This code has been placed into the public domain by its author.

package com.springie.demos;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Tim's balance rule: a judged slate must carry equal numbers heading N
 * and S, and equal numbers heading E and W.
 */
public class CompassBalanceTest {

  private static Map<String, CompassPoint> slate(Object... pairs) {
    final Map<String, CompassPoint> map = new HashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      map.put((String) pairs[i], (CompassPoint) pairs[i + 1]);
    }
    return map;
  }

  @Test
  void balancedSlatePasses() {
    assertNull(CompassBalance.checkBalanced(
        slate("a", CompassPoint.N, "b", CompassPoint.S,
              "c", CompassPoint.E, "d", CompassPoint.W)));
  }

  @Test
  void emptySlateIsBalanced() {
    assertNull(CompassBalance.checkBalanced(slate()));
  }

  @Test
  void northHeavySlateIsFlagged() {
    final String violation = CompassBalance.checkBalanced(
        slate("a", CompassPoint.N, "b", CompassPoint.N,
              "c", CompassPoint.S,
              "d", CompassPoint.E, "e", CompassPoint.W));
    assertNotNull(violation);
    assertTrue(violation.contains("N/S"), "violation: " + violation);
  }

  @Test
  void southHeavySlateIsFlagged() {
    final String violation = CompassBalance.checkBalanced(
        slate("a", CompassPoint.S, "b", CompassPoint.S,
              "c", CompassPoint.E, "d", CompassPoint.W));
    assertNotNull(violation);
    assertTrue(violation.contains("N/S"), "violation: " + violation);
  }

  @Test
  void eastHeavySlateIsFlagged() {
    final String violation = CompassBalance.checkBalanced(
        slate("a", CompassPoint.E, "b", CompassPoint.E,
              "c", CompassPoint.W,
              "d", CompassPoint.N, "e", CompassPoint.S));
    assertNotNull(violation);
    assertTrue(violation.contains("E/W"), "violation: " + violation);
  }

  @Test
  void westHeavySlateIsFlagged() {
    final String violation = CompassBalance.checkBalanced(
        slate("a", CompassPoint.W,
              "b", CompassPoint.N, "c", CompassPoint.S));
    assertNotNull(violation);
    assertTrue(violation.contains("E/W"), "violation: " + violation);
  }
}
