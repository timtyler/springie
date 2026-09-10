package com.springie.modification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.Frame;
import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.context.ContextMananger;
import com.springie.metrics.AverageChargeGetter;
import com.springie.metrics.AverageElasticityGetter;
import com.springie.metrics.AverageLengthGetter;
import com.springie.metrics.AverageRadiusGetter;
import com.springie.metrics.AverageStiffnessGetter;
import com.springie.render.Coords;

/**
 * The +/- buttons on the Properties > Scalars panel must nudge the selected
 * value by a single step: + then - returns to the starting value.
 *
 * Previously the buttons scaled multiplicatively by a percentage taken from
 * an unrelated scrollbar, and the damping pair was mismatched ("+" scaled
 * elasticity while "-" scaled damping), so the damping buttons in
 * particular did not behave.
 */
class DomeRelatedChangeDelegatorTest {

  private static void bootApp() throws Exception {
    SwingUtilities.invokeAndWait(() -> FrEnd.main(new String[0]));
    final long loaded_by = System.currentTimeMillis() + 30000;
    while (ContextMananger.getLinkManager() == null
        || ContextMananger.getLinkManager().element.isEmpty()) {
      if (System.currentTimeMillis() > loaded_by) {
        throw new IllegalStateException("startup model did not load");
      }
      Thread.sleep(500);
    }
  }

  private static void disposeFrames() {
    for (final Frame frame : Frame.getFrames()) {
      frame.dispose();
    }
  }

  private static int averageDamping() {
    return new AverageStiffnessGetter(ContextMananger.getNodeManager())
        .getAverage();
  }

  private static int averageElasticity() {
    return new AverageElasticityGetter(ContextMananger.getNodeManager())
        .getAverage();
  }

  private static int averageLengthPx() {
    return new AverageLengthGetter(ContextMananger.getNodeManager())
        .getAverage() >> Coords.shift;
  }

  private static int averageRadiusUnits() {
    return new AverageRadiusGetter(ContextMananger.getNodeManager())
        .getAverage() >> 7;
  }

  private static int averageCharge() {
    return new AverageChargeGetter(ContextMananger.getNodeManager())
        .getAverage();
  }

  private static void selectAllLinks() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> ContextMananger.getLinkManager().selectAll());
  }

  private static void selectAllNodes() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> ContextMananger.getNodeManager().selectAll());
  }

  @Test
  void dampingPlusMinusAreInverseSingleSteps() throws Exception {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    bootApp();
    try {
      selectAllLinks();
      final int before = averageDamping();

      DomeRelatedChangeDelegator.stiffnessUp();
      assertEquals(before + 1, averageDamping(),
          "damping + should increment by 1");

      DomeRelatedChangeDelegator.stiffnessDown();
      DomeRelatedChangeDelegator.stiffnessDown();
      assertEquals(before - 1, averageDamping(),
          "damping - should decrement by 1");

      DomeRelatedChangeDelegator.stiffnessUp();
      assertEquals(before, averageDamping(),
          "+ then - should return to the starting value");
    } finally {
      disposeFrames();
    }
  }

  @Test
  void dampingPlusDoesNotTouchElasticity() throws Exception {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    bootApp();
    try {
      selectAllLinks();
      final int elasticity_before = averageElasticity();

      DomeRelatedChangeDelegator.stiffnessUp();

      assertEquals(elasticity_before, averageElasticity(),
          "damping + must not change elasticity");
    } finally {
      disposeFrames();
    }
  }

  @Test
  void dampingStaysWithinSliderRange() throws Exception {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    bootApp();
    try {
      selectAllLinks();

      ContextMananger.getLinkManager().setStiffnessOfSelected(200);
      DomeRelatedChangeDelegator.stiffnessUp();
      assertEquals(200, averageDamping(),
          "damping + at the 200 maximum must stay at 200");

      ContextMananger.getLinkManager().setStiffnessOfSelected(0);
      DomeRelatedChangeDelegator.stiffnessDown();
      assertEquals(0, averageDamping(),
          "damping - at the 0 minimum must stay at 0");
    } finally {
      disposeFrames();
    }
  }

  @Test
  void elasticityPlusMinusAreInverseSingleSteps() throws Exception {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    bootApp();
    try {
      selectAllLinks();
      final int before = averageElasticity();

      DomeRelatedChangeDelegator.elasticityUp();
      assertEquals(before + 1, averageElasticity(),
          "elasticity + should increment by 1");

      DomeRelatedChangeDelegator.elasticityDown();
      assertEquals(before, averageElasticity(),
          "+ then - should return to the starting value");
    } finally {
      disposeFrames();
    }
  }

  @Test
  void lengthPlusMinusAreInverseSinglePixelSteps() throws Exception {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    bootApp();
    try {
      selectAllLinks();
      final int before_px = averageLengthPx();

      DomeRelatedChangeDelegator.lengthenLinks();
      assertEquals(before_px + 1, averageLengthPx(),
          "length + should increment by 1 pixel");

      DomeRelatedChangeDelegator.shortenLinks();
      assertEquals(before_px, averageLengthPx(),
          "+ then - should return to the starting value");
    } finally {
      disposeFrames();
    }
  }

  @Test
  void radiusPlusMinusAreInverseSingleSteps() throws Exception {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    bootApp();
    try {
      selectAllNodes();
      final int before = averageRadiusUnits();

      DomeRelatedChangeDelegator.expand();
      assertEquals(before + 1, averageRadiusUnits(),
          "radius + should increment by 1");

      DomeRelatedChangeDelegator.contract();
      assertEquals(before, averageRadiusUnits(),
          "+ then - should return to the starting value");
    } finally {
      disposeFrames();
    }
  }

  @Test
  void chargePlusMinusAreInverseSingleSteps() throws Exception {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    bootApp();
    try {
      selectAllNodes();
      final int before = averageCharge();

      DomeRelatedChangeDelegator.chargeUp();
      assertEquals(before + 1, averageCharge(),
          "charge + should increment by 1");

      DomeRelatedChangeDelegator.chargeDown();
      assertEquals(before, averageCharge(),
          "+ then - should return to the starting value");
    } finally {
      disposeFrames();
    }
  }
}
