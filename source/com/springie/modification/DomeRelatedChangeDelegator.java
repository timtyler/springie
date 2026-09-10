package com.springie.modification;

import com.springie.FrEnd;
import com.springie.context.ContextMananger;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.metrics.AverageChargeGetter;
import com.springie.metrics.AverageElasticityGetter;
import com.springie.metrics.AverageLengthGetter;
import com.springie.metrics.AverageRadiusGetter;
import com.springie.metrics.AverageStiffnessGetter;
import com.springie.modification.flags.FlagControllerDisabled;
import com.springie.modification.flags.FlagControllerFixed;
import com.springie.modification.flags.FlagControllerHidden;
import com.springie.modification.flags.FlagControllerRope;
import com.springie.modification.resize.LinkLengthEqualisation;
import com.springie.modification.resize.LinkResetter;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;

public final class DomeRelatedChangeDelegator {
  NodeManager node_manager;

  LinkManager link_manager;

  private DomeRelatedChangeDelegator() {
    // ...
  }

  public static void equaliseLinkLengths() {
    LinkLengthEqualisation tool;
    tool = new LinkLengthEqualisation(ContextMananger.getNodeManager());
    tool.equalise();
  }

  // The +/- buttons on the Properties > Scalars panel nudge the selected
  // value by a single step in slider units, clamped to the slider's range.
  // (They used to scale multiplicatively by a percentage taken from an
  // unrelated scrollbar on the Misc panel, which meant the step size was
  // unpredictable, + then - did not return to the starting value, and at
  // zero the + button did nothing at all. Worse, the damping pair was
  // mismatched: "+" scaled elasticity while "-" scaled damping.)

  // Slider ranges (maximum - visible amount) for the Scalars panel.
  private static final int DAMPING_MIN = 0;
  private static final int DAMPING_MAX = 200;
  private static final int ELASTICITY_MIN = 0;
  private static final int ELASTICITY_MAX = 350;
  private static final int LENGTH_MIN_PX = 0;
  private static final int LENGTH_MAX_PX = 9799;
  private static final int RADIUS_SHIFT = 7;
  private static final int RADIUS_MIN = 0;
  private static final int RADIUS_MAX = 799;
  private static final int CHARGE_MIN = -100;
  private static final int CHARGE_MAX = 100;

  private static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  public static void shortenLinks() {
    changeLengthBy(-1);
  }

  public static void lengthenLinks() {
    changeLengthBy(1);
  }

  private static void changeLengthBy(int delta_px) {
    FrEnd.prepareToModifyLinkTypes();
    final int average_px = new AverageLengthGetter(
        ContextMananger.getNodeManager()).getAverage() >> Coords.shift;
    final int new_px = clamp(average_px + delta_px, LENGTH_MIN_PX,
        LENGTH_MAX_PX);
    ContextMananger.getLinkManager()
        .setLengthOfSelected(new_px << Coords.shift);
  }

  public static void expand() {
    changeRadiusBy(1);
  }

  public static void contract() {
    changeRadiusBy(-1);
  }

  private static void changeRadiusBy(int delta) {
    FrEnd.prepareToModifyAllTypes();
    final int average = new AverageRadiusGetter(
        ContextMananger.getNodeManager()).getAverage() >> RADIUS_SHIFT;
    final int new_value = clamp(average + delta, RADIUS_MIN, RADIUS_MAX)
        << RADIUS_SHIFT;
    ContextMananger.getLinkManager().setRadiusOfSelected(new_value);
    ContextMananger.getNodeManager().setRadiusOfSelected(new_value);
    RendererDelegator.repaintAll();
  }

  public static void chargeDown() {
    changeChargeBy(-1);
  }

  public static void chargeUp() {
    changeChargeBy(1);
  }

  private static void changeChargeBy(int delta) {
    FrEnd.prepareToModifyNodeTypes();
    final int average = new AverageChargeGetter(
        ContextMananger.getNodeManager()).getAverage();
    ContextMananger.getNodeManager()
        .setChargeOfSelected(clamp(average + delta, CHARGE_MIN, CHARGE_MAX));
    FrEnd.postCleanup();
  }

  public static void elasticityUp() {
    changeElasticityBy(1);
  }

  public static void elasticityDown() {
    changeElasticityBy(-1);
  }

  private static void changeElasticityBy(int delta) {
    FrEnd.prepareToModifyLinkTypes();
    final int average = new AverageElasticityGetter(
        ContextMananger.getNodeManager()).getAverage();
    ContextMananger.getLinkManager().setElasticityOfSelected(
        clamp(average + delta, ELASTICITY_MIN, ELASTICITY_MAX));
  }

  public static void stiffnessUp() {
    changeDampingBy(1);
  }

  public static void stiffnessDown() {
    changeDampingBy(-1);
  }

  private static void changeDampingBy(int delta) {
    FrEnd.prepareToModifyLinkTypes();
    final int average = new AverageStiffnessGetter(
        ContextMananger.getNodeManager()).getAverage();
    ContextMananger.getLinkManager().setStiffnessOfSelected(
        clamp(average + delta, DAMPING_MIN, DAMPING_MAX));
  }

  public static void resetLinkLengths() {
    new LinkResetter(ContextMananger.getNodeManager()).reset();
  }

  public static void hide() {
    new FlagControllerHidden(ContextMananger.getNodeManager())
        .hide(FrEnd.panel_edit_properties_flags.checkbox_hidden.getState());
    FrEnd.postCleanup();
  }

  public static void fix() {
    new FlagControllerFixed(ContextMananger.getNodeManager())
        .fix(FrEnd.panel_edit_properties_flags.checkbox_pinned.getState());
  }

  public static void rope() {
    new FlagControllerRope(ContextMananger.getNodeManager())
        .rope(FrEnd.panel_edit_properties_flags.checkbox_compression.getState(), FrEnd.panel_edit_properties_flags.checkbox_tension.getState());
    FrEnd.postCleanup();
  }

  public static void disable() {
    new FlagControllerDisabled(ContextMananger.getNodeManager())
        .disable(FrEnd.panel_edit_properties_flags.checkbox_disabled.getState());
    FrEnd.postCleanup();
  }
}