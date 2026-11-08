package com.springie.io;

import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.io.in.DataInput;
import java.awt.GraphicsEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Graceful-failure tests for loading models that do not exist.
 * Loading a missing/unreachable model (e.g. the dead springie.com URLs
 * in the model index) must fail gracefully: ResourceLoader used to
 * swallow the IOException, return a null stream and die with
 * NullPointerException in getStringFromInputStream;
 * DataInput.readInSprFile then dereferenced the null translation
 * result as well.
 *
 * The tests that load the bundled preset models live in springie-gui
 * (BundledModelLoadTest): the model files belong to springie-presets,
 * which io's test classpath cannot reach (Tim, 2026-11-07).
 */
class DataInputModelLoadTest {

  private NodeManager manager;

  @BeforeEach
  void setUp() {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    this.manager = new NodeManager();
    ContextManager.setNodeManager(this.manager);
  }

  @Test
  void missingModelFileLoadDoesNotThrow() {
    // Dead model links (e.g. the defunct springie.com index entries)
    // must fail gracefully instead of throwing NullPointerException.
    // (The resource:// missing-name case is covered in springie-gui's
    // BundledModelLoadTest, where the models are actually present, so
    // the absence there is a real one.)
    final DataInput input = new DataInput(this.manager);
    assertDoesNotThrow(() -> input.loadFile("file:///nonexistent-springie-model.spr"),
        "missing model file should fail gracefully, not throw");
  }

  @Test
  void missingModelPresetMenuPathDoesNotThrow() {
    // The MSG_PRESET_CHOSEN path (NodeManager.addCreatureFromLocation)
    // dereferenced the null translation result after a failed load.
    this.manager.initialSetUp();
    assertDoesNotThrow(
        () -> this.manager.addCreatureFromLocation(
            "file:///nonexistent-springie-model.spr"),
        "missing model should fail gracefully on the preset-menu path");
  }
}
