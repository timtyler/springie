// This code has been placed into the public domain by its author

package com.springie.render.scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of everything the renderers draw: nodes, links and
 * faces as plain data in world units. Built by {@link SceneExtractor}
 * under the model lock; safe to hand to any thread.
 */
public final class ModelScene {
  public final List<SceneNode> nodes;
  public final List<SceneLink> links;
  public final List<SceneFace> faces;

  public ModelScene(final List<SceneNode> nodes,
      final List<SceneLink> links, final List<SceneFace> faces) {
    this.nodes = Collections.unmodifiableList(new ArrayList<>(nodes));
    this.links = Collections.unmodifiableList(new ArrayList<>(links));
    this.faces = Collections.unmodifiableList(new ArrayList<>(faces));
  }
}
