// * The loaded models: each lives in its own slot with its own universe.
// *
// * Switching slots swaps the active NodeManager (via ContextManager) and
// * restores that model's universe settings, so every model keeps the
// * simulation parameters it was saved -- or last left -- with.

package com.springie.context;

import java.util.ArrayList;
import java.util.List;

import com.springie.FrEnd;
import com.springie.elements.nodes.NodeManager;
import com.springie.io.in.DataInput;
import com.springie.world.UniverseState;

public final class ModelManager {
  private static final List<ModelSlot> slots = new ArrayList<>();
  private static int active_index = 0;

  private static final List<Runnable> change_listeners = new ArrayList<>();

  private ModelManager() {
    // static only
  }

  /**
   * Clears the model slots for a fresh boot. The slots are JVM-static, so
   * without this a second FrEnd.main() in the same JVM (as the GUI tests
   * do) would keep the first boot's slot, and replaceCurrentModel would
   * load the new model into that stale, invisible manager.
   */
  public static void reset() {
    slots.clear();
    active_index = 0;
  }

  /**
   * Creates the first slot from the boot model, once a node manager exists.
   * The Models menu is built before applet.init() installs the node manager,
   * so this must tolerate being called too early: leaving the slot list empty
   * and retrying on the next call. (Capturing the slot early pinned a null
   * manager, which made every later File > Load throw NPE in
   * DataInput.resetWorkspaces.)
   */
  private static void ensureInitialized() {
    if (!slots.isEmpty()) {
      return;
    }
    final NodeManager manager = ContextManager.getNodeManager();
    if (manager == null) {
      return;
    }
    final String path = FrEnd.last_file_path;
    slots.add(new ModelSlot(leafName(path), manager,
        UniverseState.capture(manager), path));
  }

  public static List<ModelSlot> getSlots() {
    ensureInitialized();
    return slots;
  }

  public static int getActiveIndex() {
    ensureInitialized();
    return active_index;
  }

  public static ModelSlot getActiveSlot() {
    ensureInitialized();
    return slots.get(active_index);
  }

  /** Switches to another loaded model, preserving each one's universe. */
  public static void switchTo(int index) {
    ensureInitialized();
    if (index < 0 || index >= slots.size() || index == active_index) {
      return;
    }

    // remember the universe we're leaving
    final ModelSlot current = slots.get(active_index);
    current.universe = UniverseState.capture(current.manager);

    active_index = index;
    final ModelSlot next = slots.get(active_index);
    ContextManager.setNodeManager(next.manager);
    next.universe.restore(next.manager);

    FrEnd.setFilePath(next.file_path);
    FrEnd.reflectAllValuesInGUIAfterSeriousEditing();
    notifyChanged();
  }

  /**
   * Loads a model file into a new slot and switches to it.
   * The current model's universe is preserved.
   */
  public static void loadNewModel(String path) {
    ensureInitialized();

    // the load clobbers the global universe statics, so remember ours first
    final ModelSlot current = slots.get(active_index);
    current.universe = UniverseState.capture(current.manager);

    final NodeManager fresh = new NodeManager();
    new DataInput(fresh).loadFile(path);
    final UniverseState universe = UniverseState.capture(fresh);

    final ModelSlot slot = new ModelSlot(leafName(path), fresh, universe, path);
    slots.add(slot);
    active_index = slots.size() - 1;

    ContextManager.setNodeManager(fresh);
    universe.restore(fresh);

    FrEnd.setFilePath(path);
    FrEnd.reflectAllValuesInGUIAfterSeriousEditing();
    notifyChanged();
  }

  /**
   * Replaces the active model's contents with a model file
   * (the previous "Load objects..." behaviour).
   */
  public static void replaceCurrentModel(String path) {
    ensureInitialized();

    final ModelSlot current = slots.get(active_index);
    new DataInput(current.manager).loadFile(path);
    current.universe = UniverseState.capture(current.manager);
    current.name = leafName(path);
    current.file_path = path;

    FrEnd.setFilePath(path);
    FrEnd.reflectAllValuesInGUIAfterSeriousEditing();
    notifyChanged();
  }

  /** Closes the active model, if another one remains. */
  public static void closeActiveSlot() {
    ensureInitialized();
    if (slots.size() < 2) {
      return;
    }

    slots.remove(active_index);
    if (active_index >= slots.size()) {
      active_index = slots.size() - 1;
    }

    final ModelSlot next = slots.get(active_index);
    ContextManager.setNodeManager(next.manager);
    next.universe.restore(next.manager);

    FrEnd.setFilePath(next.file_path);
    FrEnd.reflectAllValuesInGUIAfterSeriousEditing();
    notifyChanged();
  }

  /** Registers a callback run whenever the slot list or selection changes. */
  public static void addChangeListener(Runnable listener) {
    change_listeners.add(listener);
  }

  private static void notifyChanged() {
    for (int i = 0; i < change_listeners.size(); i++) {
      change_listeners.get(i).run();
    }
  }

  private static String leafName(String path) {
    if (path == null) {
      return "model";
    }
    final String stripped = path.startsWith("file://")
        ? path.substring("file://".length()) : path;
    final int slash = Math.max(stripped.lastIndexOf('/'),
        stripped.lastIndexOf('\\'));
    final String leaf = slash >= 0 ? stripped.substring(slash + 1) : stripped;
    return leaf.isEmpty() ? "model" : leaf;
  }

  /** Test-only: resets the slot list. */
  static void resetForTests() {
    slots.clear();
    active_index = 0;
    change_listeners.clear();
  }
}
