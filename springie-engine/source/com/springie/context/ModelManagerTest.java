// Tests for multi-model slots: each model keeps its own universe.
// The slot tests that load real model files live in springie-io
// (ModelManagerLoadTest): loading needs io's parser and the presets'
// model files, and the engine cannot depend on either (Tim, 2026-11-07).

package com.springie.context;

import com.springie.elements.nodes.NodeManager;
import com.springie.world.World;
import java.awt.GraphicsEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ModelManagerTest {

  private int saved_gravity;
  private boolean saved_gravity_active;
  private int saved_temperature;

  @BeforeEach
  void setUp() {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    saved_gravity = World.gravity_strength;
    saved_gravity_active = World.gravity_active;
    saved_temperature = World.global_temperature;

    ModelManager.resetForTests();
    ContextManager.setNodeManager(new NodeManager());
  }

  @AfterEach
  void tearDown() {
    World.gravity_strength = saved_gravity;
    World.gravity_active = saved_gravity_active;
    World.global_temperature = saved_temperature;
    ModelManager.resetForTests();
  }

  @Test
  void firstSlotWrapsTheCurrentModel() {
    final NodeManager manager = ContextManager.getNodeManager();
    final ModelSlot slot = ModelManager.getActiveSlot();

    assertEquals(1, ModelManager.getSlots().size());
    assertTrue(slot.manager == manager, "slot wraps the live manager");
  }

  @Test
  void slotCreationDefersUntilTheNodeManagerExists() {
    // The Models menu is built before FrEnd.init() installs the node
    // manager; getSlots() must not capture a null manager then (that NPE'd
    // every later File > Load in DataInput.resetWorkspaces).
    ContextManager.setNodeManager(null);
    assertEquals(0, ModelManager.getSlots().size(),
        "no slot may be created while the manager is missing");

    final NodeManager manager = new NodeManager();
    ContextManager.setNodeManager(manager);
    assertEquals(1, ModelManager.getSlots().size(),
        "the slot is created once the manager exists");
    assertTrue(ModelManager.getActiveSlot().manager == manager,
        "the slot wraps the live manager, never null");
  }

  @Test
  void closeLastSlotIsRefused() {
    ModelManager.closeActiveSlot();

    assertEquals(1, ModelManager.getSlots().size());
  }
}
