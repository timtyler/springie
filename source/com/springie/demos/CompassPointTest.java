// This code has been placed into the public domain by its author.

package com.springie.demos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The compass mapping Tim approved: E = +x, W = -x, S = +z, N = -z
 * (y grows downward; the floor plane is x/z).
 */
public class CompassPointTest {

  @AfterEach
  void restoreBiasSize() {
    // bias_size is global universe state; a test that changes it must not
    // leak into tests running later in the same JVM.
    CompassPoint.bias_size = 0;
  }

  @Test
  void eastProgressIsDx() {
    assertEquals(10, CompassPoint.E.progress(10, 3));
    assertEquals(-7, CompassPoint.E.progress(-7, 3));
  }

  @Test
  void westProgressIsNegatedDx() {
    assertEquals(-10, CompassPoint.W.progress(10, 3));
    assertEquals(7, CompassPoint.W.progress(-7, 3));
  }

  @Test
  void southProgressIsDz() {
    assertEquals(3, CompassPoint.S.progress(10, 3));
    assertEquals(-4, CompassPoint.S.progress(10, -4));
  }

  @Test
  void northProgressIsNegatedDz() {
    assertEquals(-3, CompassPoint.N.progress(10, 3));
    assertEquals(4, CompassPoint.N.progress(10, -4));
  }

  @Test
  void crabbingSidewaysScoresNothingAlongTheHeading() {
    // The post-mortem case: all motion in z while heading E.
    assertEquals(0, CompassPoint.E.progress(0, 149));
    assertEquals(0, CompassPoint.N.progress(27, 0));
  }

  @Test
  void lateralIsPerpendicularDrift() {
    assertEquals(3, CompassPoint.E.lateral(10, 3));
    assertEquals(3, CompassPoint.W.lateral(10, -3));
    assertEquals(10, CompassPoint.N.lateral(10, 3));
    assertEquals(10, CompassPoint.S.lateral(-10, 3));
  }

  @Test
  void oppositePairs() {
    assertEquals(CompassPoint.S, CompassPoint.N.opposite());
    assertEquals(CompassPoint.N, CompassPoint.S.opposite());
    assertEquals(CompassPoint.W, CompassPoint.E.opposite());
    assertEquals(CompassPoint.E, CompassPoint.W.opposite());
  }

  @Test
  void demoHeadingsAreEast() {
    assertEquals(CompassPoint.E, CrawlerDemo.compassHeading());
    assertEquals(CompassPoint.E, SpiderTankDemo.compassHeading());
    assertEquals(CompassPoint.E, CaterpillarDemo.compassHeading());
    assertEquals(CompassPoint.E, Caterpillar2Demo.compassHeading());
  }

  @Test
  void biasSizeDefaultsToZero() {
    assertEquals(0, CompassPoint.bias_size);
  }

  @Test
  void biasVelocityPointsAlongTheHeading() {
    CompassPoint.bias_size = 7;
    assertEquals(7, CompassPoint.E.biasDx());
    assertEquals(0, CompassPoint.E.biasDz());
    assertEquals(-7, CompassPoint.W.biasDx());
    assertEquals(0, CompassPoint.W.biasDz());
    assertEquals(0, CompassPoint.S.biasDx());
    assertEquals(7, CompassPoint.S.biasDz());
    assertEquals(0, CompassPoint.N.biasDx());
    assertEquals(-7, CompassPoint.N.biasDz());
  }

  @Test
  void zeroBiasAddsNoVelocity() {
    CompassPoint.bias_size = 0;
    for (final CompassPoint heading : CompassPoint.values()) {
      assertEquals(0, heading.biasDx(), heading.name());
      assertEquals(0, heading.biasDz(), heading.name());
    }
  }
}
