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
}
