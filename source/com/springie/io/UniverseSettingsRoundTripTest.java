// This program has been placed into the public domain by its author.
package com.springie.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.GraphicsEnvironment;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.io.in.DataInput;
import com.springie.io.out.Serialiser;
import com.springie.muscles.Muscles;
import com.springie.world.World;

/**
 * Universe settings travel with the model: every Universe-tab control must
 * survive a save/load round trip, and loading a model must start from the
 * defaults so attributes absent from the file cannot inherit stale values
 * from the previously loaded model.
 */
class UniverseSettingsRoundTripTest {

  private NodeManager manager;

  private int saved_gravity_strength;
  private boolean saved_gravity_active;
  private int saved_temperature;
  private int saved_minimum_magnitude;
  private int saved_viscocity;
  private int saved_max_speed;
  private boolean saved_three_d;
  private boolean saved_check_collisions;
  private boolean saved_continuously_centre;
  private boolean saved_node_growth;
  private boolean saved_charge_active;
  private boolean saved_muscles_enabled;
  private int saved_muscles_amplitude;
  private int saved_muscles_period_ticks;

  @BeforeEach
  void setUp() {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    this.manager = new NodeManager();
    ContextManager.setNodeManager(this.manager);

    this.saved_gravity_strength = World.gravity_strength;
    this.saved_gravity_active = World.gravity_active;
    this.saved_temperature = World.global_temperature;
    this.saved_minimum_magnitude = World.minimum_magnitude;
    this.saved_viscocity = Node.viscocity;
    this.saved_max_speed = Node.max_speed;
    this.saved_three_d = FrEnd.three_d;
    this.saved_check_collisions = FrEnd.check_collisions;
    this.saved_continuously_centre = FrEnd.continuously_centre;
    this.saved_node_growth = FrEnd.node_growth;
    this.saved_charge_active = this.manager.electrostatic.charge_active;
    this.saved_muscles_enabled = Muscles.enabled;
    this.saved_muscles_amplitude = Muscles.activeOscillator().getAmplitude();
    this.saved_muscles_period_ticks =
        Muscles.activeOscillator().getPeriodTicks();
  }

  @AfterEach
  void tearDown() {
    World.gravity_strength = this.saved_gravity_strength;
    World.gravity_active = this.saved_gravity_active;
    World.global_temperature = this.saved_temperature;
    World.minimum_magnitude = this.saved_minimum_magnitude;
    Node.viscocity = this.saved_viscocity;
    Node.max_speed = this.saved_max_speed;
    FrEnd.three_d = this.saved_three_d;
    FrEnd.check_collisions = this.saved_check_collisions;
    FrEnd.continuously_centre = this.saved_continuously_centre;
    FrEnd.node_growth = this.saved_node_growth;
    ContextManager.getNodeManager().electrostatic.charge_active =
        this.saved_charge_active;
    Muscles.enabled = this.saved_muscles_enabled;
    Muscles.activeOscillator().setAmplitude(this.saved_muscles_amplitude);
    Muscles.activeOscillator()
        .setPeriodTicks(this.saved_muscles_period_ticks);
  }

  private static void setUniverse(int gravity_strength,
      boolean gravity_active, int temperature, int minimum_magnitude,
      int viscocity, int max_speed, boolean three_d,
      boolean check_collisions, boolean continuously_centre,
      boolean node_growth, boolean charge_active) {
    World.gravity_strength = gravity_strength;
    World.gravity_active = gravity_active;
    World.global_temperature = temperature;
    World.minimum_magnitude = minimum_magnitude;
    Node.viscocity = viscocity;
    Node.max_speed = max_speed;
    FrEnd.three_d = three_d;
    FrEnd.check_collisions = check_collisions;
    FrEnd.continuously_centre = continuously_centre;
    FrEnd.node_growth = node_growth;
    ContextManager.getNodeManager().electrostatic.charge_active =
        charge_active;
  }

  private static void assertUniverse(int gravity_strength,
      boolean gravity_active, int temperature, int minimum_magnitude,
      int viscocity, int max_speed, boolean three_d,
      boolean check_collisions, boolean continuously_centre,
      boolean node_growth, boolean charge_active) {
    assertEquals(gravity_strength, World.gravity_strength, "gravity_strength");
    assertEquals(gravity_active, World.gravity_active, "gravity_active");
    assertEquals(temperature, World.global_temperature, "temperature");
    assertEquals(minimum_magnitude, World.minimum_magnitude, "excite");
    assertEquals(viscocity, Node.viscocity, "viscosity");
    assertEquals(max_speed, Node.max_speed, "speed limit");
    assertEquals(three_d, FrEnd.three_d, "3D");
    assertEquals(check_collisions, FrEnd.check_collisions,
        "collision check");
    assertEquals(continuously_centre, FrEnd.continuously_centre,
        "continuously centre");
    assertEquals(node_growth, FrEnd.node_growth, "node growth");
    assertEquals(charge_active,
        ContextManager.getNodeManager().electrostatic.charge_active,
        "charge active");
  }

  private static void setMuscles(boolean enabled, int amplitude,
      int period_ticks) {
    Muscles.enabled = enabled;
    Muscles.activeOscillator().setAmplitude(amplitude);
    Muscles.activeOscillator().setPeriodTicks(period_ticks);
  }

  private static void assertMuscles(boolean enabled, int amplitude,
      int period_ticks) {
    assertEquals(enabled, Muscles.enabled, "muscles enabled");
    assertEquals(amplitude, Muscles.activeOscillator().getAmplitude(),
        "muscles amplitude");
    assertEquals(period_ticks, Muscles.activeOscillator().getPeriodTicks(),
        "muscles period");
  }

  @Test
  void universeSettingsSurviveSaveAndLoad() throws Exception {
    new DataInput(this.manager)
        .loadFile("resource://models/moscow.spr");

    // Non-default universe state, saved with the model.
    setUniverse(42, true, 777, 123456, 66, 987654, false, false,
        true, true, false);
    setMuscles(true, 100, 33);
    final String spr = new Serialiser(this.manager).toString();

    // Scramble to unrelated values, proving the load overwrites them.
    setUniverse(7, false, 8, 9, 10, 11, true, true, false, false,
        true);
    setMuscles(false, 200, 44);

    // Round-trip through a real file: XML -> SAX -> tokens -> parse,
    // exactly like File > Open.
    final java.io.File temp =
        java.io.File.createTempFile("universe-round-trip", ".spr");
    try {
      final java.io.FileWriter writer = new java.io.FileWriter(temp);
      writer.write(spr);
      writer.close();
      new DataInput(this.manager)
          .loadFile("file://" + temp.getAbsolutePath());
    } finally {
      temp.delete();
    }

    assertUniverse(42, true, 777, 123456, 66, 987654, false, false,
        true, true, false);
    assertMuscles(true, 100, 33);
  }

  @Test
  void loadingOldModelResetsAbsentAttributesToDefaults() {
    // The bundled Moscow was re-saved with a full universe tag, but it
    // still predates the muscle attributes (and has no dimensions tag):
    // attributes absent from the file must not inherit stale values from
    // the previously loaded model.
    setUniverse(42, true, 777, 123456, 66, 987654, false, false,
        true, true, false);
    setMuscles(true, 100, 33);

    new DataInput(this.manager)
        .loadFile("resource://models/moscow.spr");

    // From the file...
    assertEquals(0, World.gravity_strength, "gravity_strength");
    assertFalse(World.gravity_active, "gravity_active");
    assertFalse(
        ContextManager.getNodeManager().electrostatic.charge_active,
        "charge active");
    assertEquals(6, World.global_temperature, "temperature");
    assertEquals(0, World.minimum_magnitude, "excite");
    assertEquals(0, Node.viscocity, "viscosity");
    assertEquals(Integer.MAX_VALUE, Node.max_speed, "speed limit");
    assertFalse(FrEnd.check_collisions, "collision check");
    assertFalse(FrEnd.continuously_centre, "continuously centre");
    assertFalse(FrEnd.node_growth, "node growth");
    // ...absent attributes back at the defaults.
    assertTrue(FrEnd.three_d, "3D");
    assertMuscles(false, 85 * Muscles.UNITY / 100, 12);
  }
}
