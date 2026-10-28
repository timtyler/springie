// This program has been placed into the public domain by its author.

package com.springie.modification;

import com.springie.elements.nodes.Node;

/**
 * The command execution context (Tim, 2026-10-13, Phase 1 FrEnd decoupling).
 *
 * <p>Commands (messages/) and model editing operations (modification/) use
 * this interface instead of calling FrEnd directly. FrEnd implements it.
 * This is the first step toward extracting springie-commands and
 * springie-editing as separate modules.
 */
public interface CommandContext {
  void prepareToModifyAllTypes();

  void prepareToModifyNodeTypes();

  void prepareToModifyLinkTypes();

  void prepareToModifyFaceTypes();

  void postCleanup();

  void reflectAllValuesInGUIAfterSeriousEditing();

  void reflectValuesInGUIAfterPropertyEditing();

  void updateGUIToReflectSelectionChange();

  void killAllLinks(final Node node);

  void greyStepIfNeeded();

  void setFilePath(final String path);

  // Panel reflect methods (Tim, 2026-10-13, Phase 2).
  void reflectCharge();
  void reflectCompass();
  void reflectElasticity();
  void reflectLength();
  void reflectPhase();
  void reflectRadius();
  void reflectDamping();

  // Generate panel values (Tim, 2026-10-13, Phase 2).
  String getGenerateFreeNodes();
  String getGenerateMatrixX();
  String getGenerateMatrixY();
  String getGenerateMatrixZ();
  String getGenerateSpherePack();
  String getGenerateStringLength();
  String getGenerateTubeCircumference();
  String getGenerateTubeLength();

  // Misc panel values (Tim, 2026-10-13, Phase 2).
  boolean getSplitLinksRemoveOld();
  String getAddNodesInnerEden();
  String getAddPolygons();
  String getAddStellations();
  String getAddTriaxialNodes();
  String getAutomaticLinkRadius();
  String getAutomaticNodeRadius();
  String getLinkNearestNNodes();
  String getSplitLinks();

  // Select advanced panel values (Tim, 2026-10-13, Phase 2).
  String getSelectFacesWithNSides();

  // App state (Tim, 2026-10-13, Phase 2).
  String getNextFilePath();
  int getGeneration();
  void incrementGeneration();
  int getColourPickerColour();
  // Flags panel (Tim, 2026-10-13, Phase 2).
  boolean getFlagCompression();
  boolean getFlagDisabled();
  boolean getFlagHidden();
  boolean getFlagPinned();
  boolean getFlagTension();
  // Transfer utilities (Tim, 2026-10-13, Phase 2).
  int[] transferLinkLengths();
  int[] transferLinkRadii();
  int[] transferNodeRadii();
}
