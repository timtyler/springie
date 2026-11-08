// Regression tests for loading the bundled .spr preset models
// (Tim, 2026-11-07): these live in springie-gui, not springie-io,
// because they load resource://models/... and the model files belong
// to springie-presets -- io's own test classpath has no models in it,
// so in io every load came back empty and these tests failed (or, for
// the missing-model ones, passed vacuously). The gui module depends on
// both io and presets. The graceful-failure tests that need no bundled
// model stay in io's DataInputModelLoadTest.
//
// What these guard:
// - ReaderTens used element.get(size) instead of element.get(size - 1)
//   when fetching a freshly added polygon, so any model containing
//   faces (Soccer ball, Icosahedron) died with
//   IndexOutOfBoundsException.
// - A failed load must not poison subsequent loads on the same manager.

package com.springie.gui.components;

import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.io.in.DataInput;
import java.awt.GraphicsEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BundledModelLoadTest {

  private NodeManager manager;

  @BeforeEach
  void setUp() {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    this.manager = new NodeManager();
    ContextManager.setNodeManager(this.manager);
  }

  @ParameterizedTest
  @ValueSource(strings = {"asm_32a", "cube", "icosahedron", "moscow", "soccer", "t_sphere_32"})
  void bundledModelLoadsWithoutException(final String name) {
    final DataInput input = new DataInput(this.manager);
    assertDoesNotThrow(() -> input.loadFile("resource://models/" + name + ".spr"),
        name + " should load without throwing");

    assertTrue(this.manager.element.size() > 0, name + ": expected nodes to load");
  }

  @ParameterizedTest
  @ValueSource(strings = {"icosahedron", "soccer"})
  void modelsWithFacesLoadTheirFaces(final String name) {
    new DataInput(this.manager).loadFile("resource://models/" + name + ".spr");

    assertTrue(this.manager.getFaceManager().element.size() > 0,
        name + ": expected faces to load");
  }

  @Test
  void failedLoadDoesNotPoisonSubsequentLoad() {    // Simulates the preset-menu sequence "Soccer ball" then "Moscow":
    // even if the first load throws, the second load on the same
    // (shared, app-global) manager must still work.
    final DataInput input = new DataInput(this.manager);

    try {
      input.loadFile("resource://models/soccer.spr");
    } catch (RuntimeException expected) {
      // the load itself is allowed to fail here; isolation is what's tested
    }

    assertDoesNotThrow(() -> input.loadFile("resource://models/moscow.spr"),
        "Moscow should load after a failed Soccer ball load");
    assertTrue(this.manager.element.size() > 0, "Moscow: expected nodes to load");
  }

  @Test
  void presetMenuPathLoadsModelsInSequence() {
    // Simulates the MSG_PRESET_CHOSEN flow used by the preset menu:
    // reset, then addCreatureFromLocation (which goes through
    // ReaderSPR.translate -> DataInput.addFromString, no loadFile).
    this.manager.initialSetUp();
    assertDoesNotThrow(
        () -> this.manager.addCreatureFromLocation("resource://models/soccer.spr"),
        "Soccer ball should load via the preset-menu path");
    assertTrue(this.manager.getFaceManager().element.size() > 0,
        "Soccer ball: expected faces to load");

    this.manager.initialSetUp();
    assertDoesNotThrow(
        () -> this.manager.addCreatureFromLocation("resource://models/moscow.spr"),
        "Moscow should load via the preset-menu path after Soccer ball");
    assertTrue(this.manager.element.size() > 0, "Moscow: expected nodes to load");
  }

  @Test
  void missingBundledModelLoadDoesNotThrow() {
    // A resource:// name that does not exist must fail gracefully
    // instead of throwing NullPointerException -- tested here, where
    // the models really are on the classpath, so "missing" is a real
    // absence and not just an unreachable classpath.
    final DataInput input = new DataInput(this.manager);
    assertDoesNotThrow(() -> input.loadFile("resource://models/does-not-exist.spr"),
        "missing bundled model should fail gracefully, not throw");
  }
}
