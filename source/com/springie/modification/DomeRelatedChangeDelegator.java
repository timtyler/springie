package com.springie.modification;

import com.springie.FrEnd;
import com.springie.context.ContextMananger;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.modification.flags.FlagControllerDisabled;
import com.springie.modification.flags.FlagControllerFixed;
import com.springie.modification.flags.FlagControllerHidden;
import com.springie.modification.flags.FlagControllerRope;
import com.springie.modification.resize.ChargeChanger;
import com.springie.modification.resize.LinkElasticityChanger;
import com.springie.modification.resize.LinkLengthChanger;
import com.springie.modification.resize.LinkLengthEqualisation;
import com.springie.modification.resize.LinkRadiusChanger;
import com.springie.modification.resize.LinkResetter;
import com.springie.modification.resize.LinkStiffnessChanger;
import com.springie.modification.resize.NodeSizeChanger;
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

  private static float getScalingScaleFactor() {
    final int value = FrEnd.panel_edit_properties_scale_factor.scroll_bar_scale_by.getValue();
    return value / 100F;
  }

  public static void shortenLinks() {
    final float scale_factor = 1F - getScalingScaleFactor();

    final LinkLengthChanger tool = new LinkLengthChanger(ContextMananger.getNodeManager());
    tool.resize(scale_factor);
  }

  public static void lengthenLinks() {
    final float scale_factor = 1F + getScalingScaleFactor();

    final LinkLengthChanger tool = new LinkLengthChanger(ContextMananger.getNodeManager());
    tool.resize(scale_factor);
  }

  public static void expand() {
    final float scale_factor = 1F + getScalingScaleFactor();

    if (ContextMananger.getNodeManager().isSelection()) {
      new NodeSizeChanger(ContextMananger.getNodeManager()).resize(scale_factor);
      RendererDelegator.repaintAll();
    }

    if (ContextMananger.getLinkManager().isSelection()) {
      new LinkRadiusChanger(ContextMananger.getNodeManager()).resize(scale_factor);
      RendererDelegator.repaintAll();
    }
  }

  public static void contract() {
    final float scale_factor = 1F - getScalingScaleFactor();
    if (ContextMananger.getNodeManager().isSelection()) {
      new NodeSizeChanger(ContextMananger.getNodeManager()).resize(scale_factor);
      RendererDelegator.repaintAll();
    }

    if (ContextMananger.getLinkManager().isSelection()) {
      new LinkRadiusChanger(ContextMananger.getNodeManager()).resize(scale_factor);
      RendererDelegator.repaintAll();
    }
  }

  public static void chargeDown() {
    final float scale_factor = 1F - getScalingScaleFactor();
    new ChargeChanger(ContextMananger.getNodeManager()).scaleBy(scale_factor);
    FrEnd.postCleanup();
  }

  public static void chargeUp() {
    final float scale_factor = 1F + getScalingScaleFactor();
    new ChargeChanger(ContextMananger.getNodeManager()).scaleBy(scale_factor);
    FrEnd.postCleanup();
  }

  public static void elasticityUp() {
    final float scale_factor = 1F + getScalingScaleFactor();
    final LinkElasticityChanger tool = new LinkElasticityChanger(
        ContextMananger.getNodeManager());
    tool.resize(scale_factor);
  }

  public static void elasticityDown() {
    final float scale_factor = 1F - getScalingScaleFactor();
    final LinkElasticityChanger tool = new LinkElasticityChanger(
        ContextMananger.getNodeManager());
    tool.resize(scale_factor);
  }

  public static void stiffnessUp() {
    final float scale_factor = 1F + getScalingScaleFactor();
    final LinkElasticityChanger tool = new LinkElasticityChanger(
        ContextMananger.getNodeManager());
    tool.resize(scale_factor);
  }

  public static void stiffnessDown() {
    final float scale_factor = 1F - getScalingScaleFactor();
    final LinkStiffnessChanger tool = new LinkStiffnessChanger(
        ContextMananger.getNodeManager());
    tool.resize(scale_factor);
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