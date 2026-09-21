// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.FrEnd;
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
 * Parametric 4-legged crawler built from tetrahedra.
 *
 * <p>Body: a rigid hull built from two tetrahedral blocks sharing the
 * bottom-diagonal edge (b1, b3) -- Tet(b0, b1, b3, t0) and
 * Tet(b1, b2, b3, t1) -- plus the ridge tie (t0, t1) and a fully
 * triangulated bottom plate. 13 bars for 6 nodes (12 needed), so the
 * chassis is a rigid 3D truss: it holds its shape instead of folding.
 *
 * <p>Each leg is a rigid volumetric tetrahedron (hip1, hip2, knee, foot)
 * attached to the body via a shared edge (two nodes), never a single
 * corner. All six edges of the leg tetrahedron are passive struts, so
 * the leg is a rigid paddle that swings about its hip edge like a
 * pendulum -- it cannot fold up under the body. The leg's one muscle is
 * a CABLE (a tension member -- it pulls but never pushes) from the
 * ridge node down to the foot: contracting it lifts the paddle, gravity
 * drops it again. One muscle per leg.
 *
 * <p>A trot gait (diagonal legs in phase) lifts and swings the feet.
 * Node-node collisions are OFF: the model holds together through its
 * structure alone.
 *
 * <p>Parameters are public fields so the judge can sweep them.
 */
public final class CrawlerDemo {
  private CrawlerDemo() {
  }

  /** Edge length of body tetrahedra, in pixels. */
  public static int body_edge_px = 60;
  /** Leg length (hip to foot), in pixels. */
  public static int leg_length_px = 30;
  /** Leg splay (hip to knee sideways), in pixels. */
  public static int leg_splay_px = 8;
  /** Knee forward offset (+x), in pixels. */
  public static int knee_forward_px = 12;
  /** Foot forward offset (+x), in pixels. */
  public static int foot_forward_px = 25;
  /** Elasticity of the stiff body-skeleton struts. */
  public static int body_elasticity = 70;
  /** Elasticity of the leg springs (stays in the non-resonant band). */
  public static int leg_elasticity = 30;
  /** Muscle amplitude, 0-100%. */
  public static int muscle_amplitude_pct = 12;
  /**
   * Tim's "not tipping over" rule: element indices of the dorsal "top"
   * node (ridge t0) and the "bottom" reference node (base b0). The top
   * must stay above the bottom for the whole run.
   */
  public static final int posture_top_index = 4;
  public static final int posture_bottom_index = 0;
  /** Min top-above-bottom separation (px) for the tip-over rule. */
  public static final int posture_min_separation_px = 15;
  /** Muscle period, in ticks. */
  public static int muscle_period_ticks = 120;
  /** Ground friction, 0-100. */
  public static int friction = 100;

  /**
   * The model's intended direction of travel on the floor plane.
   * The crawler walks toward +x: East.
   */
  public static CompassPoint compassHeading() {
    return CompassPoint.E;
  }

  /** Phase offsets (ticks) for FL, FR, BL, BR legs. */
  public static int[] leg_phases = {0, 60, 60, 0};

  /**
   * Builds the crawler centred at (x_px, 0) with feet on the ground.
   * Returns the body centre node for tracking.
   */
  public static Node buildAt(int x_px) {
    final NodeManager node_manager = ContextManager.getNodeManager();
    node_manager.initial_reset();
    final LinkManager link_manager = node_manager.getLinkManager();
    link_manager.reset();
    node_manager.getFaceManager().reset();

    final Clazz clazz = node_manager.clazz_factory.getNew(0xFFFFFFFF);
    final NodeType node_type = node_manager.node_type_factory.getNew();
    final int body_e = body_edge_px << Coords.shift;
    final LinkType body_type = link_manager.link_type_factory.getNew(body_e, body_elasticity);
    final LinkType leg_type = link_manager.link_type_factory.getNew(
        leg_length_px << Coords.shift, leg_elasticity);

    // Muscles.
    Muscles.enabled = true;
    Muscles.active_oscillator = 0;
    Muscles.activeOscillator().setAmplitude(muscle_amplitude_pct * Muscles.UNITY / 100);
    Muscles.activeOscillator().setPeriodTicks(muscle_period_ticks);
    Muscles.activeOscillator().setPhase(0);
    World.gravity_active = true;
    World.gravity_strength = 5; // Strong gravity to keep it grounded.
    World.ground_friction = friction;
    // No node-node collisions: the structure holds itself together.
    FrEnd.check_collisions = false;

    final int x0 = x_px << Coords.shift;
    // Ground is the high-Y wall (positive gravity pulls toward +Y).
    // Feet rest ~2px above the true wall; the whole build is shifted
    // +40px in z so no node starts against the z = 0 wall.
    final int ground = (Coords.y_pixels << Coords.shift) - (2 << Coords.shift);
    final int zo = 40 << Coords.shift;

    // Body: rigid hull from two tetrahedral blocks.
    // Bottom plate: rectangle of 4 nodes, fully triangulated.
    // Ridge: t0 over the front half, t1 over the back half.
    // Tet A = (b0, b1, b3, t0), Tet B = (b1, b2, b3, t1),
    // sharing the plate-diagonal edge (b1, b3); the ridge tie (t0, t1)
    // locks the two blocks against hinging about that edge.
    final int bw = body_e; // body width (z)
    final int bl = body_e * 2; // body length (x)
    final int bh = (int) (body_e * 0.8); // body height (y)

    // Y increases downward; ground is at high Y, so "up" is smaller Y.
    final int body_y = ground - (leg_length_px << Coords.shift) - (bh / 2);
    final Node b0 = addNode(node_manager, clazz, node_type, x0, body_y, zo);
    final Node b1 = addNode(node_manager, clazz, node_type, x0 + bl, body_y, zo);
    final Node b2 = addNode(node_manager, clazz, node_type, x0 + bl, body_y, bw + zo);
    final Node b3 = addNode(node_manager, clazz, node_type, x0, body_y, bw + zo);
    // Ridge (top).
    final Node t0 = addNode(node_manager, clazz, node_type, x0 + bl / 2, body_y - bh, zo);
    final Node t1 = addNode(node_manager, clazz, node_type, x0 + bl / 2, body_y - bh, bw + zo);

    // Bottom plate: 4 sides + 2 diagonals (rigid).
    strut(link_manager, body_type, clazz, b0, b1);
    strut(link_manager, body_type, clazz, b1, b2);
    strut(link_manager, body_type, clazz, b2, b3);
    strut(link_manager, body_type, clazz, b3, b0);
    strut(link_manager, body_type, clazz, b0, b2);
    strut(link_manager, body_type, clazz, b1, b3);
    // Tet A = (b0, b1, b3, t0): the plate edges already exist.
    strut(link_manager, body_type, clazz, b0, t0);
    strut(link_manager, body_type, clazz, b1, t0);
    strut(link_manager, body_type, clazz, b3, t0);
    // Tet B = (b1, b2, b3, t1): the plate edges already exist.
    strut(link_manager, body_type, clazz, b1, t1);
    strut(link_manager, body_type, clazz, b2, t1);
    strut(link_manager, body_type, clazz, b3, t1);
    // Ridge tie: locks the two tetrahedra against hinging.
    strut(link_manager, body_type, clazz, t0, t1);

    // Legs: each attaches to the body via an EDGE (two nodes), forming
    // a rigid volumetric tetrahedron (hip1, hip2, knee, foot) -- 4 nodes,
    // 6 edges, all passive struts. The leg is a stiff paddle hinged at
    // its hip edge: it swings fore-aft like a pendulum but cannot fold.
    // The leg's one muscle is a CABLE from the ridge node down to the
    // foot (a marionette string): contracting it lifts the paddle,
    // gravity is the antagonist that drops it. Cables pull but never
    // push, so the drive cannot shove the leg.
    // FL: edge (b0,b3), FR: edge (b3,b2), BL: edge (b1,b0), BR: edge (b2,b1).
    final Node[][] hip_edges = {{b0, b3}, {b3, b2}, {b1, b0}, {b2, b1}};
    final int[] sides = {-1, 1, -1, 1}; // splay direction (z)
    final Node[] ridge_nodes = {t0, t1};
    for (int leg = 0; leg < 4; leg++) {
      final Node hip1 = hip_edges[leg][0];
      final Node hip2 = hip_edges[leg][1];
      final int side = sides[leg];
      final int phase = leg_phases[leg];
      // Ridge node on the leg's side, for the lift cable.
      final Node ridge = ridge_nodes[(side + 1) / 2];

      // Knee: forward (+x) and halfway down; foot further forward at the
      // ground; small z splay clears the hull. (Sideways-splayed knees
      // form a shallow inverted-V that buckles under load.)
      final int mid_x = (hip1.pos.x + hip2.pos.x) / 2;
      final int mid_y = (hip1.pos.y + hip2.pos.y) / 2;
      final int mid_z = (hip1.pos.z + hip2.pos.z) / 2;
      final Node knee = addNode(node_manager, clazz, node_type,
          mid_x + (knee_forward_px << Coords.shift),
          mid_y + (leg_length_px << Coords.shift) / 2,
          mid_z + side * (leg_splay_px << Coords.shift));
      // Foot: forward (+X), just above the ground.
      final Node foot = addNode(node_manager, clazz, node_type,
          mid_x + (foot_forward_px << Coords.shift),
          ground,
          mid_z + side * (leg_splay_px << Coords.shift));

      // Rigid tetrahedral paddle (hip1, hip2, knee, foot): all 6 edges
      // are passive struts. hip1-hip2 is a body edge (already exists).
      strut(link_manager, leg_type, clazz, hip1, knee);
      strut(link_manager, leg_type, clazz, hip2, knee);
      strut(link_manager, leg_type, clazz, hip1, foot);
      strut(link_manager, leg_type, clazz, hip2, foot);
      strut(link_manager, leg_type, clazz, knee, foot);
      // The lift cable is the leg's one muscle.
      cableMuscle(link_manager, leg_type, clazz, ridge, foot, phase);
    }

    // No mid-air starts: rest the whole model on the ground plane.
    Grounding.restOnGround(node_manager);

    // Return a body node for tracking.
    return b0;
  }

  private static Node addNode(NodeManager nm, Clazz clazz, NodeType nt, int x, int y, int z) {
    return nm.addNewAgent(new Point3D(x, y, z), clazz, nt);
  }

  /**
   * Passive strut (compression member): resists both stretch and squash.
   * Rest length = actual distance, so the frame starts unstressed.
   */
  private static void strut(LinkManager lm, LinkType template, Clazz clazz, Node a, Node b) {
    // Each link gets its own type so the rest length matches this link's
    // actual geometry exactly.
    final int dist = distance(a, b);
    final LinkType type = lm.link_type_factory.getNew(dist, template.elasticity);
    type.damping = template.damping;
    final Link link = lm.setLink(a, b, type, clazz);
    link.adjusted_rest_length = dist;
  }

  /**
   * Muscle cable (tension member): pulls but never pushes. When the
   * oscillator lengthens its rest length past the actual length it
   * simply goes slack.
   */
  private static void cableMuscle(LinkManager lm, LinkType template, Clazz clazz,
      Node a, Node b, int phase) {
    final int dist = distance(a, b);
    final LinkType type = lm.link_type_factory.getNew(dist, template.elasticity);
    type.damping = template.damping;
    type.compression = false; // cable: no push when shorter than rest
    type.tension = true; // cable: pulls when longer than rest
    final Link link = lm.setLink(a, b, type, clazz);
    link.adjusted_rest_length = dist;
    link.phase = phase;
    link.controller = new GlobalOscillatorController(Muscles.active_oscillator);
  }

  private static int distance(Node a, Node b) {
    final int dx = a.pos.x - b.pos.x;
    final int dy = a.pos.y - b.pos.y;
    final int dz = a.pos.z - b.pos.z;
    return (int) Math.sqrt((long) dx * dx + (long) dy * dy + (long) dz * dz);
  }
}
