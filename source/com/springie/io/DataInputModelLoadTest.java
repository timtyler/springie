package com.springie.io;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.GraphicsEnvironment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.springie.context.ContextMananger;
import com.springie.elements.nodes.NodeManager;
import com.springie.io.in.DataInput;

/**
 * Regression tests for loading the bundled .spr preset models.
 *
 * - ReaderTens used element.get(size) instead of element.get(size - 1)
 *   when fetching a freshly added polygon, so any model containing faces
 *   (Soccer ball, Icosahedron) died with IndexOutOfBoundsException.
 * - A failed load must not poison subsequent loads on the same manager.
 * - Loading a missing/unreachable model (e.g. the dead springie.com URLs in
 *   the model index) must fail gracefully: ResourceLoader used to swallow
 *   the IOException, return a null stream and die with NullPointerException
 *   in getStringFromInputStream; DataInput.readInSprFile then dereferenced
 *   the null translation result as well.
 */
class DataInputModelLoadTest {

  private NodeManager manager;

  @BeforeEach
  void setUp() {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    this.manager = new NodeManager();
    ContextMananger.setNodeManager(this.manager);
  }

  @ParameterizedTest
  @ValueSource(strings = {"icosahedron", "moscow", "soccer", "t_sphere_32"})
  void bundledModelLoadsWithoutException(String name) {
    final DataInput input = new DataInput(this.manager);
    assertDoesNotThrow(() -> input.loadFile("resource://models/" + name + ".spr"),
        name + " should load without throwing");

    assertTrue(this.manager.element.size() > 0, name + ": expected nodes to load");
  }

  @ParameterizedTest
  @ValueSource(strings = {"icosahedron", "soccer"})
  void modelsWithFacesLoadTheirFaces(String name) {
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

  @ParameterizedTest
  @ValueSource(strings = {
      "file:///nonexistent-springie-model.spr",
      "resource://models/does-not-exist.spr"})
  void missingModelLoadDoesNotThrow(String location) {
    // Dead model links (e.g. the defunct springie.com index entries) must
    // fail gracefully instead of throwing NullPointerException.
    final DataInput input = new DataInput(this.manager);
    assertDoesNotThrow(() -> input.loadFile(location),
        location + " should fail gracefully, not throw");
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
