// * One loaded model: its nodes/links/faces plus the universe it lives in.
// *
// * The NodeManager owns the model contents; the UniverseState owns the
// * simulation parameters that were current when the model was saved (or
// * when it was last the active model).

package com.springie.context;

import com.springie.elements.nodes.NodeManager;
import com.springie.world.UniverseState;

public final class ModelSlot {
  /** Display name, usually the file leaf. */
  public String name;

  /** The model's nodes, links, faces and per-model state. */
  public final NodeManager manager;

  /** The universe settings belonging to this model. */
  public UniverseState universe;

  /** Full path used for the window title and saving. */
  public String file_path;

  public ModelSlot(String name, NodeManager manager, UniverseState universe,
      String file_path) {
    this.name = name;
    this.manager = manager;
    this.universe = universe;
    this.file_path = file_path;
  }
}
