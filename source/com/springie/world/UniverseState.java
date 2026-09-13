// * A snapshot of the universe settings that travel with a model.
// *
// * The universe is the set of simulation parameters stored in statics
// * (World, Node, FrEnd) plus the per-NodeManager charge flag. Each loaded
// * model keeps its own snapshot so switching models restores the universe
// * that model was saved (or last left) with.

package com.springie.world;

import com.springie.FrEnd;
import com.springie.elements.electrostatics.ElectrostaticRepulsion;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.muscles.Muscles;

public final class UniverseState {
  public int gravity_strength;
  public boolean gravity_active;
  public int global_temperature;
  public int minimum_magnitude;

  public int viscocity;
  public int max_speed;

  public boolean three_d;
  public boolean check_collisions;
  public boolean links_disabled;
  public boolean continuously_centre;
  public boolean node_growth;

  public boolean charge_active;

  public boolean muscles_enabled;
  public int muscles_amplitude;
  public int muscles_period_ticks;

  private UniverseState() {
    // use capture()
  }

  /** Reads the current universe settings into a new snapshot. */
  public static UniverseState capture(NodeManager manager) {
    final UniverseState state = new UniverseState();

    state.gravity_strength = World.gravity_strength;
    state.gravity_active = World.gravity_active;
    state.global_temperature = World.global_temperature;
    state.minimum_magnitude = World.minimum_magnitude;

    state.viscocity = Node.viscocity;
    state.max_speed = Node.max_speed;

    state.three_d = FrEnd.three_d;
    state.check_collisions = FrEnd.check_collisions;
    state.links_disabled = FrEnd.links_disabled;
    state.continuously_centre = FrEnd.continuously_centre;
    state.node_growth = FrEnd.node_growth;

    final ElectrostaticRepulsion electrostatic = manager != null
        ? manager.electrostatic : null;
    state.charge_active = electrostatic != null && electrostatic.charge_active;

    state.muscles_enabled = Muscles.enabled;
    state.muscles_amplitude = Muscles.activeOscillator().getAmplitude();
    state.muscles_period_ticks = Muscles.activeOscillator().getPeriodTicks();

    return state;
  }

  /** Writes this snapshot back, becoming the current universe. */
  public void restore(NodeManager manager) {
    World.gravity_strength = this.gravity_strength;
    World.gravity_active = this.gravity_active;
    World.global_temperature = this.global_temperature;
    World.minimum_magnitude = this.minimum_magnitude;

    Node.viscocity = this.viscocity;
    Node.max_speed = this.max_speed;

    FrEnd.three_d = this.three_d;
    FrEnd.check_collisions = this.check_collisions;
    FrEnd.links_disabled = this.links_disabled;
    FrEnd.continuously_centre = this.continuously_centre;
    FrEnd.node_growth = this.node_growth;

    if (manager != null) {
      manager.electrostatic.charge_active = this.charge_active;
    }

    Muscles.enabled = this.muscles_enabled;
    Muscles.activeOscillator().setAmplitude(this.muscles_amplitude);
    Muscles.activeOscillator().setPeriodTicks(this.muscles_period_ticks);
  }
}
