// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.context.ContextManager;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.links.LinkType;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.elements.nodes.NodeType;
import com.springie.geometry.Point3D;
import com.springie.muscles.GlobalOscillatorController;
import com.springie.muscles.Muscles;
import com.springie.render.Coords;
import com.springie.world.Grounding;
import com.springie.world.World;

/**
 * Caterpillar track (Tim, 2026-09-25): a ring of proper volumetric
 * tetrahedra, like a tank tread.
 *
 * <p>Structure: 12 segments around the circle, 36 nodes total. Three
 * rings: inner (I, in the wheel plane, z=0), left outer (L, z=+30),
 * right outer (R, z=-30).
 *
 * <p>Each segment i contributes TWO proper tetrahedra:
 * <ul>
 * <li>Tetra A: (I[i], I[i+1], L[i], R[i])</li>
 * <li>Tetra B: (I[i], I[i+1], L[i+1], R[i+1])</li>
 * </ul>
 * The two tetras share the inner edge I[i]-I[i+1] (edge-join).
 * Consecutive segments share the face (I[i+1], L[i+1], R[i+1])
 * (face-join). All joins are edge or face -- never single-corner.
 *
 * <p>Each tetrahedron is VOLUMETRIC: the four nodes span z=-30 to +30
 * and are offset in-plane, giving significant 3D volume. Not a
 * stabilized square (near-planar quad with cross-bracing).
 *
 * <p>Drive: the 12 inner-circle links (I[i]-I[i+1]) are strut muscles.
 * Each has a phase offset so the contraction wave runs twice around
 * the circle (f=2). The other 5 edges of each tetrahedron are passive
 * struts. (Tim: struts everywhere for this model, even with muscles.)
 *
 * <p>Tim's plan: stabilize the direction with an axle (to be added later).
 *
 * <p>Node order: inner[0..11], left[0..11], right[0..11] (element indices
 * 0-11, 12-23, 24-35). buildAt returns inner[0].
 */
public final class CaterpillarTrackDemo {
  private CaterpillarTrackDemo() {
  }

  /** Nodes per ring (Tim: "thirty-something nodes" total). */
  public static final int SEGMENTS = 12;

  /** Inner circle radius, in pixels. */
  public static final int INNER_RADIUS_PX = 120;

  /** Outer circle radius, in pixels. */
  public static final int OUTER_RADIUS_PX = 140;

  /** Track half-width (z offset of outer rings), in pixels. */
  public static final int HALF_WIDTH_PX = 30;

  /** Node size, in pixels (physical radius). */
  public static int node_size_px = 16;

  /** Muscle wave frequency: full sine waves around the circle. */
  public static final int WAVE_FREQUENCY = 2;

  /** Muscle amplitude, as a percentage. */
  public static int muscle_amplitude_pct = 85;

  /** Muscle oscillator period, in ticks. */
  public static int muscle_period_ticks = 120;

  /** Link elasticity. */
  public static int elasticity = 50;

  /** Ground friction. */
  public static int friction = 100;

  /** Log mass for all nodes. */
  public static int log_mass = 0;

  /**
   * Builds the caterpillar track centred at the given x (pixels),
   * resting on the ground. Returns the first inner node.
   */
  public static Node buildAt(int x_px) {
    final NodeManager node_manager = ContextManager.getNodeManager();
    node_manager.initial_reset();
    final LinkManager link_manager = node_manager.getLinkManager();
    link_manager.reset();
    node_manager.getFaceManager().reset();

    final Clazz clazz = node_manager.clazz_factory.getNew(0xFFFFFFFF);
    final NodeType node_type = node_manager.node_type_factory.getNew();
    node_type.log_mass = log_mass;
    node_type.setSize(node_size_px);

    // The muscle wave runs on oscillator slot 0.
    Muscles.enabled = true;
    Muscles.active_oscillator = 0;
    Muscles.activeOscillator().setAmplitude(
        muscle_amplitude_pct * Muscles.UNITY / 100);
    Muscles.activeOscillator().setPeriodTicks(muscle_period_ticks);
    Muscles.activeOscillator().setPhase(0);
    World.gravity_active = true;
    World.gravity_strength = 2;
    World.ground_friction = friction;
    World.global_temperature = 0;

    // Ground is the high-Y wall (positive gravity pulls toward +Y).
    // The ground line sits one node radius above the canvas floor, so
    // node centres start exactly at their rest height.
    final int ground_y_px = Coords.y_pixels - node_size_px - 1;
    final int centre_y_px = ground_y_px - OUTER_RADIUS_PX;
    final int centre_y = centre_y_px << Coords.shift;
    final int cx = x_px << Coords.shift;

    // Create the nodes: inner[0..11], left[0..11], right[0..11].
    final Node[] inner = new Node[SEGMENTS];
    final Node[] left = new Node[SEGMENTS];
    final Node[] right = new Node[SEGMENTS];
    final int half_width = HALF_WIDTH_PX << Coords.shift;

    for (int i = 0; i < SEGMENTS; i++) {
      final double angle = 2.0 * Math.PI * i / SEGMENTS;
      final int dx_inner =
          (int) (INNER_RADIUS_PX * Math.cos(angle)) << Coords.shift;
      final int dy_inner =
          (int) (INNER_RADIUS_PX * Math.sin(angle)) << Coords.shift;
      final int dx_outer =
          (int) (OUTER_RADIUS_PX * Math.cos(angle)) << Coords.shift;
      final int dy_outer =
          (int) (OUTER_RADIUS_PX * Math.sin(angle)) << Coords.shift;

      // Inner node: in the wheel plane (z=0).
      inner[i] = node_manager.addNewAgent(
          new Point3D(cx + dx_inner, centre_y + dy_inner, 0),
          clazz, node_type);

      // Left outer node: displaced +z.
      left[i] = node_manager.addNewAgent(
          new Point3D(cx + dx_outer, centre_y + dy_outer, half_width),
          clazz, node_type);

      // Right outer node: displaced -z.
      right[i] = node_manager.addNewAgent(
          new Point3D(cx + dx_outer, centre_y + dy_outer, -half_width),
          clazz, node_type);
    }

    // The muscle controller for the inner-circle wave.
    final GlobalOscillatorController controller =
        new GlobalOscillatorController(0);

    // Inner circle: 12 strut muscle links with f=2 phase wave.
    // Link i connects I[i]-I[i+1]; phase is 2 full cycles around.
    // These are the shared edges of the tetrahedra pairs.
    // Tim: struts everywhere for this model, even with muscles.
    for (int i = 0; i < SEGMENTS; i++) {
      final int j = (i + 1) % SEGMENTS;
      final LinkType type = link_manager.link_type_factory.getNew(
          distance(inner[i], inner[j]), elasticity);
      // Strut (compression enabled), with muscle.
      final Link link = link_manager.setLink(inner[i], inner[j], type, clazz);
      link.adjusted_rest_length = type.length;
      // Phase in ticks: f=2 waves around SEGMENTS links.
      link.phase = (WAVE_FREQUENCY * i * muscle_period_ticks) / SEGMENTS;
      link.controller = controller;
    }

    // Build the tetrahedra. Each segment i has two:
    //   Tetra A: (I[i], I[i+1], L[i], R[i])
    //   Tetra B: (I[i], I[i+1], L[i+1], R[i+1])
    // The I[i]-I[i+1] edge is the muscle (above). The other 5 edges
    // of each tetra are passive struts.
    for (int i = 0; i < SEGMENTS; i++) {
      final int j = (i + 1) % SEGMENTS;

      // Tetra A: (I[i], I[j], L[i], R[i]).
      // Edges (I[i]-I[j] is the muscle, not a strut):
      strut(link_manager, clazz, inner[i], left[i]);
      strut(link_manager, clazz, inner[i], right[i]);
      strut(link_manager, clazz, inner[j], left[i]);
      strut(link_manager, clazz, inner[j], right[i]);
      strut(link_manager, clazz, left[i], right[i]);

      // Tetra B: (I[i], I[j], L[j], R[j]).
      strut(link_manager, clazz, inner[i], left[j]);
      strut(link_manager, clazz, inner[i], right[j]);
      strut(link_manager, clazz, inner[j], left[j]);
      strut(link_manager, clazz, inner[j], right[j]);
      strut(link_manager, clazz, left[j], right[j]);
    }

    // Rest on the ground (no mid-air start, per Tim's grounding rule).
    Grounding.restOnGround(node_manager);

    return inner[0];
  }

  /** Builds the track at the default x position. */
  public static Node build() {
    return buildAt(300);
  }

  /** A passive strut link (compression+tensor) between two nodes. */
  private static void strut(LinkManager lm, Clazz clazz, Node a, Node b) {
    final LinkType type =
        lm.link_type_factory.getNew(distance(a, b), elasticity);
    // Strut: compression enabled (default).
    final Link link = lm.setLink(a, b, type, clazz);
    link.adjusted_rest_length = type.length;
  }

  /** Geometric distance between two nodes, in world units. */
  private static int distance(Node a, Node b) {
    final int dx = a.pos.x - b.pos.x;
    final int dy = a.pos.y - b.pos.y;
    final int dz = a.pos.z - b.pos.z;
    return (int) Math.sqrt((long) dx * dx + (long) dy * dy + (long) dz * dz);
  }
}
