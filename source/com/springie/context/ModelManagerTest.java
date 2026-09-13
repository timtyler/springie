// Tests for multi-model slots: each model keeps its own universe.

package com.springie.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.GraphicsEnvironment;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.world.UniverseState;
import com.springie.world.World;

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
    // The Models menu is built before applet.init() installs the node
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
  void loadNewModelAddsASlotAndSwitchesToIt() {
    final ModelSlot first = ModelManager.getActiveSlot();

    ModelManager.loadNewModel("resource://models/cube.spr");

    assertEquals(2, ModelManager.getSlots().size());
    assertEquals(1, ModelManager.getActiveIndex());
    final ModelSlot second = ModelManager.getActiveSlot();
    assertNotSame(first.manager, second.manager);
    assertTrue(ContextManager.getNodeManager() == second.manager);
    assertEquals("cube.spr", second.name);
    assertEquals(12, second.manager.element.size());
  }

  @Test
  void switchRestoresEachModelsUniverse() {
    ModelManager.loadNewModel("resource://models/cube.spr");
    final ModelSlot first = ModelManager.getSlots().get(0);
    final ModelSlot second = ModelManager.getSlots().get(1);

    // give each model a distinct universe
    World.gravity_strength = 5;
    World.global_temperature = 10;
    first.universe = UniverseState.capture(first.manager);

    ModelManager.switchTo(1);
    World.gravity_strength = 25;
    World.global_temperature = 30;
    second.universe = UniverseState.capture(second.manager);

    // switching restores the stored universe
    ModelManager.switchTo(0);
    assertEquals(5, World.gravity_strength);
    assertEquals(10, World.global_temperature);
    assertTrue(ContextManager.getNodeManager() == first.manager);

    ModelManager.switchTo(1);
    assertEquals(25, World.gravity_strength);
    assertEquals(30, World.global_temperature);
    assertTrue(ContextManager.getNodeManager() == second.manager);
  }

  @Test
  void switchCapturesTheUniverseBeingLeft() {
    ModelManager.loadNewModel("resource://models/cube.spr");

    World.gravity_strength = 42;
    ModelManager.switchTo(0);

    // the value we left behind was captured into the slot
    ModelManager.switchTo(1);
    assertEquals(42, World.gravity_strength);
  }

  @Test
  void closeActiveSlotKeepsTheOtherModel() {
    ModelManager.loadNewModel("resource://models/cube.spr");
    final ModelSlot first = ModelManager.getSlots().get(0);

    ModelManager.closeActiveSlot();

    assertEquals(1, ModelManager.getSlots().size());
    assertEquals(0, ModelManager.getActiveIndex());
    assertTrue(ModelManager.getActiveSlot() == first);
    assertTrue(ContextManager.getNodeManager() == first.manager);
  }

  @Test
  void closeLastSlotIsRefused() {
    ModelManager.closeActiveSlot();

    assertEquals(1, ModelManager.getSlots().size());
  }

  @Test
  void replaceCurrentModelKeepsTheSlot() {
    final ModelSlot slot = ModelManager.getActiveSlot();
    final NodeManager manager = slot.manager;

    ModelManager.replaceCurrentModel("resource://models/cube.spr");

    assertEquals(1, ModelManager.getSlots().size());
    assertTrue(ModelManager.getActiveSlot() == slot);
    assertTrue(slot.manager == manager, "same manager, new contents");
    assertEquals(12, manager.element.size());
    assertEquals("cube.spr", slot.name);
  }
}
