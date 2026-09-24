// This code has been placed into the public domain by its author.

package com.springie.world;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.demos.CompassPoint;
import com.springie.elements.nodes.Node;
import com.springie.muscles.Muscles;

/**
 * Snapshot of the universe settings belonging to the current model.
 *
 * <p>Each model (demo or file) sets its own universe parameters when it
 * loads -- gravity, friction, charge, collisions, muscle tuning, and so
 * on. The Universe tab's "Reset universe" button must restore THOSE
 * settings, not some hardcoded factory default. This class captures the
 * model's settings when the model loads ({@link #snapshot()}) and writes
 * them back on reset ({@link #restore()}).
 *
 * <p>The snapshot is initialised to the factory defaults, so a reset
 * before any model loads behaves as it always did.
 */
public final class UniverseDefaults {
  private UniverseDefaults() {
    // static-only
  }

  // 3D view.
  private static boolean three_d = true;

  // Gravity.
  private static int gravity_strength = 2;
  private static boolean gravity_active = false;

  // Friction.
  private static int ground_friction = 0;

  // Temperature.
  private static int global_temperature = 6;

  // Viscosity.
  private static int viscocity = 0;

  // Collision checks.
  private static boolean check_collisions = true;

  // Link forces (false means enabled).

  // Charge.
  private static boolean charge_active = true;

  // Muscles.
  private static boolean muscles_enabled = false;
  private static int active_oscillator = 0;
  private static int muscle_amplitude = 85 * Muscles.UNITY / 100;
  private static int muscle_period_ticks = 12;
  private static int muscle_phase = 0;

  // Continuously centre, per axis.
  private static boolean continuously_centre_x = false;
  private static boolean continuously_centre_y = false;
  private static boolean continuously_centre_z = false;

  // Development-only: speed limit and minimum excitation magnitude.
  private static int max_speed = Integer.MAX_VALUE;
  private static int minimum_magnitude = 0;

  // Compass bias size (velocity units added per frame along the heading).
  private static int compass_bias_size = 0;

  /**
   * Resets the snapshot to the factory defaults. For tests: the snapshot
   * is global state, and a test that loads a model would otherwise pollute
   * the snapshot for tests that run later in the same JVM.
   */
  public static void resetToFactoryDefaults() {
    three_d = true;
    gravity_strength = 2;
    gravity_active = false;
    ground_friction = 0;
    global_temperature = 6;
    viscocity = 0;
    check_collisions = true;
    charge_active = true;
    muscles_enabled = false;
    active_oscillator = 0;
    muscle_amplitude = 85 * Muscles.UNITY / 100;
    muscle_period_ticks = 12;
    muscle_phase = 0;
    continuously_centre_x = false;
    continuously_centre_y = false;
    continuously_centre_z = false;
    max_speed = Integer.MAX_VALUE;
    minimum_magnitude = 0;
    compass_bias_size = 0;
  }

  /**
   * Captures the current universe statics as the current model's defaults.
   * Call this once the model has finished setting its universe parameters.
   */
  public static void snapshot() {
    three_d = FrEnd.three_d;
    gravity_strength = World.gravity_strength;
    gravity_active = World.gravity_active;
    ground_friction = World.ground_friction;
    global_temperature = World.global_temperature;
    viscocity = Node.viscocity;
    check_collisions = FrEnd.check_collisions;
    charge_active = ContextManager.getNodeManager().electrostatic.charge_active;
    muscles_enabled = Muscles.enabled;
    active_oscillator = Muscles.active_oscillator;
    muscle_amplitude = Muscles.activeOscillator().getAmplitude();
    muscle_period_ticks = Muscles.activeOscillator().getPeriodTicks();
    muscle_phase = Muscles.activeOscillator().getPhase();
    continuously_centre_x = FrEnd.continuously_centre_x;
    continuously_centre_y = FrEnd.continuously_centre_y;
    continuously_centre_z = FrEnd.continuously_centre_z;
    compass_bias_size = CompassPoint.bias_size;
    if (FrEnd.development_version) {
      max_speed = Node.max_speed;
      minimum_magnitude = World.minimum_magnitude;
    }
  }

  /**
   * Writes the snapshot back to the universe statics. Does not touch the
   * GUI controls -- the caller reflects afterwards.
   */
  public static void restore() {
    FrEnd.three_d = three_d;
    World.gravity_strength = gravity_strength;
    World.gravity_active = gravity_active;
    World.ground_friction = ground_friction;
    World.global_temperature = global_temperature;
    Node.viscocity = viscocity;
    FrEnd.check_collisions = check_collisions;
    ContextManager.getNodeManager().electrostatic.charge_active = charge_active;
    Muscles.enabled = muscles_enabled;
    Muscles.active_oscillator = active_oscillator;
    Muscles.activeOscillator().setAmplitude(muscle_amplitude);
    Muscles.activeOscillator().setPeriodTicks(muscle_period_ticks);
    Muscles.activeOscillator().setPhase(muscle_phase);
    FrEnd.continuously_centre_x = continuously_centre_x;
    FrEnd.continuously_centre_y = continuously_centre_y;
    FrEnd.continuously_centre_z = continuously_centre_z;
    CompassPoint.bias_size = compass_bias_size;
    if (FrEnd.development_version) {
      Node.max_speed = max_speed;
      World.minimum_magnitude = minimum_magnitude;
    }
  }
}
