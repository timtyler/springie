// This code has been placed into the public domain by its author.

package com.springie.demos;

import java.util.LinkedHashMap;
import java.util.Map;

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
import com.springie.muscles.Controller;
import com.springie.muscles.GlobalOscillatorController;
import com.springie.muscles.Muscles;
import com.springie.render.Coords;
import com.springie.world.Grounding;
import com.springie.world.World;

/**
 * Parametric 4-legged crawler built from tetrahedra (second generation).
 *
 * <p>Body: a rigid hull built from two tetrahedral blocks sharing the
 * bottom-diagonal edge (b1, b3) -- Tet(b0, b1, b3, t0) and
 * Tet(b1, b2, b3, t1) -- plus the ridge tie (t0, t1) and a fully
 * triangulated bottom plate. 13 bars for 6 nodes (12 needed), so the
 * chassis is a rigid 3D truss: it holds its shape instead of folding.
 *
 * <p>Each leg is a rigid volumetric tetrahedron (hip1, hip2, knee, foot)
 * hinged on a TRANSVERSE body edge (two nodes) -- front legs on the
 * front edge (b0, b3), rear legs on the rear edge (b1, b2) -- so every
 * leg swings fore-aft like a pendulum. Never a single corner, never a
 * longitudinal edge (which would swing the leg sideways). All five
 * leg struts are passive; the leg cannot fold up under the body.
 *
 * <p>Drive: two antagonistic CABLE muscles per leg (tension members --
 * they pull but never push). The protraction cable runs from a body
 * anchor ahead of the foot to the foot (swings the unloaded foot
 * forward and up); the retraction cable runs from a body anchor behind
 * the foot to the foot -- during stance, with the foot planted, it
 * hauls the body forward over the foot: the power stroke. Feet stand
 * well inboard of the hip edges, so every leg has a good anchor on
 * both sides. A trot gait (diagonal legs in phase) walks the machine
 * toward +x (East).
 *
 * <p>Directional stability comes from a balanced compass bias, not from
 * the gait: the north-side leg nodes (knee, foot of FL and BL) get an N
 * nudge each step and the south-side leg nodes an equal S nudge
 * ({@link HeadingStabilizerController}). Equal numbers N and S -- zero
 * E, zero W -- so the net bias is exactly zero: it is a stabilizer,
 * not a motor. Yawed by an angle, the pull-apart pair produces a
 * yaw-restoring torque, damping heading wander.
 *
 * <p>Node-node collisions are OFF: the model holds together through its
 * structure alone.
 *
 * <p>Parameters are public fields so the judge can sweep them.
 */
public final class CrawlerDemo {
  private CrawlerDemo() {
  }

  /** Body edge length, in pixels. */
  public static int body_edge_px = 90;
  /** Ridge height above the plate, in pixels. */
  public static int ridge_height_px = 72;
  /** Hip height above the ground, in pixels. */
  public static int hip_height_px = 55;
  /** Leg splay (hip to knee sideways), in pixels. */
  public static int leg_splay_px = 14;
  /** Knee forward offset (+x), in pixels. */
  public static int knee_forward_px = 10;
  /**
   * Foot inboard offset, in pixels: front feet sit this far behind the
   * front hip edge, rear feet this far ahead of the rear hip edge, so
   * every foot stands well inside the body's footprint with a body
   * anchor ahead of it (protraction) and one behind it (the stance
   * power stroke). Symmetric fore/aft, so the passive model stands
   * still instead of vaulting forward.
   */
  public static int foot_inboard_px = 40;
  /** Elasticity of the stiff body-skeleton struts. */
  public static int body_elasticity = 70;
  /** Elasticity of the leg springs (stays in the non-resonant band). */
  public static int leg_elasticity = 30;
  /** Muscle amplitude, 0-100%. */
  public static int muscle_amplitude_pct = 6;
  /**
   * Tim's "not tipping over" rule: element indices of the dorsal "top"
   * node (ridge t0) and the "bottom" reference node (base b0). The top
   * must stay above the bottom for the whole run.
   */
  public static final int posture_top_index = 4;
  public static final int posture_bottom_index = 0;
  /** Min top-above-bottom separation (px) for the tip-over rule. */
  public static int posture_min_separation_px = 15;
  /** Muscle period, in ticks. */
  public static int muscle_period_ticks = 120;
  /**
  /** Ground friction, 0-100. */
  public static int friction = 100;
  /**
   * Heading-stabilizer bias, in internal velocity units per frame
   * (256 units = 1 px/frame). N on the north-side leg nodes, S on the
   * south-side leg nodes; equal counts, so the net bias is zero.
   * Small: a stabilizer, not a motor. 0 disables.
   */
  public static int heading_stabilizer_bias = 2;
  /**
   * Minimum clearance (px) of the body's centre of gravity above the
   * floor, sustained over the whole judged run. A low-riding body
   * counts as a failure even if it never quite touches.
   */
  public static int cog_min_clearance_px = 50;

  /**
   * The model's intended direction of travel on the floor plane.
   * The crawler walks toward +x: East.
   */
  public static CompassPoint compassHeading() {
    return CompassPoint.E;
  }

  /** Phase offsets (ticks) for FL, FR, BL, BR legs (trot: diagonals). */
  public static int[] leg_phases = {0, 60, 60, 0};

  /**
   * The compass-bias layout applied by the last {@link #buildAt} call:
   * biased node label to bias direction. The judge checks it is
   * balanced (equal N/S, equal E/W) -- the layout is part of the demo
   * definition. Rebuilt on every build.
   */
  public static Map<String, CompassPoint> last_bias_layout =
      new LinkedHashMap<String, CompassPoint>();

  /**
   * Builds the crawler with its front feet at x_px and its feet on the
   * ground. Returns the body centre node for tracking.
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
        hip_height_px << Coords.shift, leg_elasticity);

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

    final int x0 = (x_px + foot_inboard_px) << Coords.shift;
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
    final int bh = ridge_height_px << Coords.shift; // body height (y)

    // Y increases downward; ground is at high Y, so "up" is smaller Y.
    // The hip edges (front (b0,b3), rear (b1,b2)) sit hip_height above
    // the ground; the ridge rides above the plate.
    final int plate_y = ground - (hip_height_px << Coords.shift);
    final Node b0 = addNode(node_manager, clazz, node_type, x0, plate_y, zo);
    final Node b1 = addNode(node_manager, clazz, node_type, x0 + bl, plate_y, zo);
    final Node b2 = addNode(node_manager, clazz, node_type, x0 + bl, plate_y, bw + zo);
    final Node b3 = addNode(node_manager, clazz, node_type, x0, plate_y, bw + zo);
    // Ridge (top).
    final Node t0 = addNode(node_manager, clazz, node_type, x0 + bl / 2, plate_y - bh, zo);
    final Node t1 = addNode(node_manager, clazz, node_type, x0 + bl / 2, plate_y - bh, bw + zo);

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
    final Link first_body_link =
        strut(link_manager, body_type, clazz, t0, t1);

    // Legs: each hinges on a TRANSVERSE body edge -- front legs on the
    // front edge (b0, b3), rear legs on the rear edge (b1, b2) -- so
    // every leg swings fore-aft. Each leg is a rigid volumetric
    // tetrahedron (hip1, hip2, knee, foot): 4 nodes, 6 edges, all
    // passive struts (hip1-hip2 is the body edge). The leg is a stiff
    // paddle pendulum: it swings about its hip edge but cannot fold.
    //
    // Drive: two antagonistic CABLE muscles per leg. The protraction
    // cable (body anchor ahead of the foot -> foot) swings the unloaded
    // foot forward and up; the retraction cable (body anchor behind the
    // foot -> foot) is the stance power stroke -- with the foot planted,
    // shortening it hauls the body forward over the foot. Cables pull
    // but never push, so the pair never fights itself: while one pulls,
    // the other goes slack. Feet stand inboard of the hip edges, giving
    // every leg a good anchor on both sides: for a front leg the hip
    // node is the protraction anchor and the ridge node the retraction
    // anchor; for a rear leg it is the other way round.
    final Node[][] hip_edges = {{b0, b3}, {b0, b3}, {b1, b2}, {b1, b2}};
    final Node[] hip_nodes = {b0, b3, b1, b2}; // anchor on the leg's side
    final Node[] ridge_nodes = {t0, t1, t0, t1}; // anchor on the leg's side
    final int[] sides = {-1, 1, -1, 1}; // splay direction (z): N, S, N, S
    final String[] names = {"FL", "FR", "BL", "BR"};
    final Node[] north_leg_nodes = new Node[4];
    final Node[] south_leg_nodes = new Node[4];
    int north_count = 0;
    int south_count = 0;
    last_bias_layout = new LinkedHashMap<String, CompassPoint>();
    // Compass-bias layout: north-side leg nodes get N, south-side
    // get S. Recorded for the judge; the net bias is zero.
    final int period = muscle_period_ticks;
    for (int leg = 0; leg < 4; leg++) {
      final Node hip1 = hip_edges[leg][0];
      final Node hip2 = hip_edges[leg][1];
      final int side = sides[leg];
      final boolean front = leg < 2;
      final int leg_phase = ((leg_phases[leg] % period) + period) % period;
      // Protraction peaks mid-swing, retraction mid-stance: the cable
      // pulls hardest when its rest length is shortest, i.e. at
      // tick + phase = 3/4 period.
      final int protract_phase = leg_phase;
      final int retract_phase = (leg_phase + period / 2) % period;
      // Front leg: hip node ahead of the foot (protraction), ridge
      // node behind it (power stroke). Rear leg: the other way round.
      final Node protract_anchor = front ? hip_nodes[leg] : ridge_nodes[leg];
      final Node retract_anchor = front ? ridge_nodes[leg] : hip_nodes[leg];

      final int mid_x = (hip1.pos.x + hip2.pos.x) / 2;
      final int mid_y = (hip1.pos.y + hip2.pos.y) / 2;
      final int mid_z = (hip1.pos.z + hip2.pos.z) / 2;
      // Knee: slightly forward, halfway down. (Sideways-splayed knees
      // form a shallow inverted-V that buckles under load.)
      final Node knee = addNode(node_manager, clazz, node_type,
          mid_x + (knee_forward_px << Coords.shift),
          mid_y + ((hip_height_px << Coords.shift)) / 2,
          mid_z + side * (leg_splay_px << Coords.shift));
      // Foot: inboard of the hip edge (toward the body's middle), just
      // above the ground, splayed clear of the hull.
      final int foot_x = front ? mid_x - (foot_inboard_px << Coords.shift)
          : mid_x + (foot_inboard_px << Coords.shift);
      final Node foot = addNode(node_manager, clazz, node_type,
          foot_x,
          ground,
          mid_z + side * (leg_splay_px << Coords.shift));

      // Rigid tetrahedral paddle (hip1, hip2, knee, foot): all 6 edges
      // are passive struts. hip1-hip2 is a body edge (already exists).
      strut(link_manager, leg_type, clazz, hip1, knee);
      strut(link_manager, leg_type, clazz, hip2, knee);
      strut(link_manager, leg_type, clazz, hip1, foot);
      strut(link_manager, leg_type, clazz, hip2, foot);
      strut(link_manager, leg_type, clazz, knee, foot);
      // Antagonistic cable pair: protraction swings the foot forward,
      // retraction drives the stance power stroke.
      cableMuscle(link_manager, leg_type, clazz, protract_anchor, foot,
          protract_phase);
      cableMuscle(link_manager, leg_type, clazz, retract_anchor, foot,
          retract_phase);

      // Compass-bias layout: north-side leg nodes get N, south-side
      // get S. Recorded for the judge; the net bias is zero.
      if (side < 0) {
        north_leg_nodes[north_count++] = knee;
        north_leg_nodes[north_count++] = foot;
        last_bias_layout.put(names[leg] + "-knee", CompassPoint.N);
        last_bias_layout.put(names[leg] + "-foot", CompassPoint.N);
      } else {
        south_leg_nodes[south_count++] = knee;
        south_leg_nodes[south_count++] = foot;
        last_bias_layout.put(names[leg] + "-knee", CompassPoint.S);
        last_bias_layout.put(names[leg] + "-foot", CompassPoint.S);
      }
    }

    // Heading stabilizer: N bias on the north leg nodes, S bias on
    // the south leg nodes. One instance on one passive link, so the
    // bias applies exactly once per dynamics step.
    first_body_link.controller = new HeadingStabilizerController(
        trim(north_leg_nodes, north_count), trim(south_leg_nodes, south_count),
        heading_stabilizer_bias);

    // No mid-air starts: rest the whole model on the ground plane.
    Grounding.restOnGround(node_manager);

    // Return a body node for tracking.
    return b0;
  }

  private static Node[] trim(Node[] nodes, int count) {
    final Node[] trimmed = new Node[count];
    System.arraycopy(nodes, 0, trimmed, 0, count);
    return trimmed;
  }

  private static Node addNode(NodeManager nm, Clazz clazz, NodeType nt, int x, int y, int z) {
    return nm.addNewAgent(new Point3D(x, y, z), clazz, nt);
  }

  /**
   * Passive strut (compression member): resists both stretch and squash.
   * Rest length = actual distance, so the frame starts unstressed.
   */
  private static Link strut(LinkManager lm, LinkType template, Clazz clazz, Node a, Node b) {
    // Each link gets its own type so the rest length matches this link's
    // actual geometry exactly.
    final int dist = distance(a, b);
    final LinkType type = lm.link_type_factory.getNew(dist, template.elasticity);
    type.damping = template.damping;
    final Link link = lm.setLink(a, b, type, clazz);
    link.adjusted_rest_length = dist;
    return link;
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

  /**
   * Heading stabilizer for the crawler (Tim's directive: use directional
   * bias to provide a stable direction; the judge checks the net bias
   * is zero).
   *
   * <p>Every dynamics step it nudges the north-side leg nodes toward N
   * (velocity.z -= bias) and the south-side leg nodes toward S
   * (velocity.z += bias) -- i.e. the two sides are pulled <em>apart</em>
   * laterally. Equal node counts make the net force exactly zero, so
   * the crawler is never shoved sideways or forward; while walking
   * straight the two biases are collinear with the body's lateral axis,
   * producing no torque (only a slight lateral tension the leg struts
   * absorb).
   *
   * <p>Yawed by an angle theta, the pull-apart pair is no longer
   * symmetric about the heading: the torque about the vertical works
   * out to -2 * bias * width * sin(theta), i.e. yaw-restoring. (The
   * swapped assignment, sides pulled together, flips the sign and is
   * yaw-amplifying -- the same finding as the wheel's axle
   * stabilizer.) The stabilizer cannot turn the crawler around: the
   * 180-degree yawed orientation is an unstable equilibrium, so it only
   * damps wander around the initial heading.
   *
   * <p>Why a {@link Controller} that writes node velocities instead of
   * {@code adjusted_rest_length}: the Controller interface is the only
   * per-dynamics-step hook the engine offers a demo (LinkManager calls
   * {@code update} once per step per link carrying a controller). A
   * heading nudge is a force, not a rest-length change, so this
   * controller ignores its link and writes velocities directly. Attach
   * exactly one instance to exactly one link so the bias applies once
   * per frame.
   */
  public static final class HeadingStabilizerController implements Controller {
    private final Node[] north_nodes;
    private final Node[] south_nodes;
    private final int bias;

    /**
     * @param north_nodes leg nodes on the north side (smaller z)
     * @param south_nodes leg nodes on the south side (larger z)
     * @param bias nudge size in internal velocity units per frame
     *             (256 units = 1 px/frame)
     */
    public HeadingStabilizerController(Node[] north_nodes, Node[] south_nodes,
        int bias) {
      this.north_nodes = north_nodes;
      this.south_nodes = south_nodes;
      this.bias = bias;
    }

    @Override
    public void update(Link link, long tick) {
      // N bias on the north side, S bias on the south side: each side
      // is pulled outward laterally. Compass mapping: N = -z, S = +z.
      for (int i = 0; i < this.north_nodes.length; i++) {
        this.north_nodes[i].velocity.z -= this.bias;
      }
      for (int i = 0; i < this.south_nodes.length; i++) {
        this.south_nodes[i].velocity.z += this.bias;
      }
    }
  }
}
