// This program has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.context.ContextManager;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.links.LinkType;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.elements.nodes.NodeType;
import com.springie.geometry.Coords;
import com.springie.geometry.Point3D;
import com.springie.elements.CompassPoint;
import com.springie.muscles.GlobalOscillatorController;
import com.springie.muscles.Muscles;
import com.springie.world.World;
import com.springie.world.Grounding;

/**
 * Rocking horse: a triangular prism athlete.
 *
 * <p>Two triangles (front and back) with the apex up, linked by squares
 * (the side rectangles) with 2 diagonal cross-links on the base. Muscles
 * on the 4 main slanting-up struts (apex to base corners). The two apex
 * nodes are connected by an axle with N/S compass stabilization.
 *
 * <p>Phasing (Tim, 2026-10-15): the front pair of slant struts runs in
 * sync (phase 0); the back pair runs 90 degrees (24% of period) out of
 * sync, producing the front-back rocking gait. Left and right stay in
 * sync so it doesn't tip sideways.
 *
 * <p>The N/S compass on the axle ends provides tip-over stabilization
 * (Tim, 2026-09-28); the tension goes through the axle, not the frame.
 */
public final class RockingHorseDemo {
  private RockingHorseDemo() {
  }

  /** Triangle base half-width, in pixels. */
  public static int base_half_width_px = 80;

  /** Triangle height (base to apex), in pixels. */
  public static int height_px = 120;

  /** Front-to-back depth (prism length), in pixels. */
  public static int depth_px = 160;

  /** Ground friction, 0-100. */
  public static int friction = 100;

  /** Gravity strength. */
  public static int gravity_strength = 5;

  /** Muscle amplitude, percent. */
  public static int muscle_amplitude_pct = 4; // Tim, 2026-10-15.

  /** Muscle period, in ticks. */
  public static int muscle_period_ticks = 36; // Optimized 2026-10-15.

  /**
   * Back-pair phase offset, in ticks. 90 degrees = 24% of the 48-tick
   * period (Tim, 2026-10-15).
   */
  public static int back_phase_ticks = 12;

  /** Per-node compass bias for the axle ends, in velocity units/frame. */
  public static int compass_bias = 20;

  /** Builds the rocking horse at the given x position. Returns the front apex. */
  public static Node buildAt(final int x_px) {
    final NodeManager node_manager = ContextManager.getNodeManager();
    node_manager.initial_reset();
    final LinkManager link_manager = node_manager.getLinkManager();
    link_manager.reset();
    node_manager.getFaceManager().reset();

    // Nodes and links get different pastel colors, same radius (Tim, 2026-10-15).
    final Clazz node_clazz = node_manager.clazz_factory.getNew(0xADD8E6);
    final Clazz link_clazz = node_manager.clazz_factory.getNew(0xFFB6C1);
    final NodeType node_type = node_manager.node_type_factory.getNew();
    node_type.log_mass = 20;
    final int radius = 16 << Coords.shift; // Doubled (Tim, 2026-10-15).
    node_type.radius = radius;

    World.gravity_active = true;
    World.gravity_strength = gravity_strength;
    com.springie.elements.CompassPoint.bias_size = 40; // Tim, 2026-10-15: 40% for visible N/S effect.
    World.ground_friction = friction;
    World.bounding_box_bounciness = 0; // Rebounds off (Tim, 2026-10-15).
    World.global_temperature = 0;
    // Use the setter (Tim, 2026-10-16): keeps ambient in sync so the
    // display doesn't go black.
    com.springie.render.modules.modern.LightSource.setColoredLighting(false);

    // Muscles: all four slant struts share one oscillator (Tim's rule).
    Muscles.enabled = true;
    Muscles.active_oscillator = 0;
    Muscles.activeOscillator().setAmplitude(
        muscle_amplitude_pct * Muscles.UNITY / 100);
    Muscles.activeOscillator().setPeriodTicks(muscle_period_ticks);
    Muscles.activeOscillator().setPhase(0);

    final int shift = Coords.shift;
    final int x = x_px << shift;
    final int z_front = 0;
    final int z_back = depth_px << shift;
    // Ground is the high-Y wall; apex up.
    final int ground = (Coords.y_pixels << shift) - (8 << shift);
    final int base_y = ground;
    final int apex_y = ground - (height_px << shift);
    final int left_x = x - (base_half_width_px << shift);
    final int right_x = x + (base_half_width_px << shift);
    final int mid_x = x;
    final int mid_z = depth_px << (shift - 1);

    // Front triangle: apex up.
    final Node f_apex = addNode(node_manager, node_clazz, node_type, mid_x, apex_y, z_front);
    final Node f_left = addNode(node_manager, node_clazz, node_type, left_x, base_y, z_front);
    final Node f_right = addNode(node_manager, node_clazz, node_type, right_x, base_y, z_front);

    // Back triangle: apex up.
    final Node b_apex = addNode(node_manager, node_clazz, node_type, mid_x, apex_y, z_back);
    final Node b_left = addNode(node_manager, node_clazz, node_type, left_x, base_y, z_back);
    final Node b_right = addNode(node_manager, node_clazz, node_type, right_x, base_y, z_back);

    // N/S compass on the apexes for tip-over stabilization.
    f_apex.compass = CompassPoint.N;
    b_apex.compass = CompassPoint.S;

    // Muscles share one oscillator (Tim's rule).
    final GlobalOscillatorController muscle =
        new GlobalOscillatorController(Muscles.active_oscillator);

    // Front triangle edges.
    passive(link_manager, link_clazz, f_apex, f_left, radius);
    passive(link_manager, link_clazz, f_apex, f_right, radius);
    muscleLink(link_manager, link_clazz, f_left, f_right, muscle, 0, radius);
    // Back triangle edges (passive).
    passive(link_manager, link_clazz, b_apex, b_left, radius);
    passive(link_manager, link_clazz, b_apex, b_right, radius);
    muscleLink(link_manager, link_clazz, b_left, b_right, muscle, back_phase_ticks, radius);

    // Axle: front apex to back apex.
    final LinkType axle_type = link_manager.link_type_factory.getNew();
    axle_type.length = depth_px << shift;
    axle_type.radius = radius;
    final Link axle = link_manager.setLink(f_apex, b_apex, axle_type, link_clazz);
    axle.adjusted_rest_length = axle_type.length;

    // Side squares: left and right.
    passive(link_manager, link_clazz, f_left, b_left, radius);
    passive(link_manager, link_clazz, f_right, b_right, radius);

    // 2 diagonal cross-links on the base rectangle.
    passive(link_manager, link_clazz, f_left, b_right, radius);
    passive(link_manager, link_clazz, f_right, b_left, radius);
    // 2 diagonal cross-links on the left square (Tim, 2026-10-15).
    passive(link_manager, link_clazz, f_apex, b_left, radius);
    passive(link_manager, link_clazz, f_left, b_apex, radius);
    // 2 diagonal cross-links on the right square (Tim, 2026-10-15).
    passive(link_manager, link_clazz, f_apex, b_right, radius);
    passive(link_manager, link_clazz, f_right, b_apex, radius);

    // Muscles on the 4 main slanting-up struts.
    // Front pair in sync (phase 0); back pair 90 degrees out.
    muscleLink(link_manager, link_clazz, f_apex, f_left, muscle, 0, radius);
    muscleLink(link_manager, link_clazz, f_apex, f_right, muscle, 0, radius);
    muscleLink(link_manager, link_clazz, b_apex, b_left, muscle, back_phase_ticks, radius);
    muscleLink(link_manager, link_clazz, b_apex, b_right, muscle, back_phase_ticks, radius);

    // Settle on the ground.
    Grounding.restOnGround(node_manager);

    return f_apex;
  }

  private static Node addNode(final NodeManager nm, final Clazz clazz,
      final NodeType nt, final int x, final int y, final int z) {
    return nm.addNewAgent(new Point3D(x, y, z), clazz, nt);
  }

  private static void passive(final LinkManager lm, final Clazz link_clazz,
      final Node a, final Node b, final int radius) {
    final LinkType type = lm.link_type_factory.getNew();
    type.length = distance(a, b);
    type.radius = radius;
    final Link link = lm.setLink(a, b, type, link_clazz);
    link.adjusted_rest_length = type.length;
  }

  private static void muscleLink(final LinkManager lm, final Clazz link_clazz,
      final Node a, final Node b, final GlobalOscillatorController muscle,
      final int phase, final int radius) {
    final LinkType type = lm.link_type_factory.getNew();
    type.length = distance(a, b);
    type.radius = radius;
    final Link link = lm.setLink(a, b, type, link_clazz);
    link.adjusted_rest_length = type.length;
    link.controller = muscle;
    link.phase = phase;
  }

  private static int distance(final Node a, final Node b) {
    final long dx = (long) a.pos.x - b.pos.x;
    final long dy = (long) a.pos.y - b.pos.y;
    final long dz = (long) a.pos.z - b.pos.z;
    return (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
  }
}
