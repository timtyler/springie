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
 * Parametric 4-legged crawler built from tetrahedra (third generation --
 * table architecture).
 *
 * <p>Body: two volumetric tetrahedral blocks sharing a face --
 * Tet(b0, b1, b3, T) and Tet(b1, b2, b3, T), sharing the face
 * (b1, b3, T). A face-shared pair of tetrahedra is a rigid truss, so
 * the chassis holds its shape instead of folding. Node layout (x grows
 * toward the direction of travel, y grows downward, z grows south):
 * b0 rear-north, b1 front-north, b2 front-south, b3 rear-south, T ridge.
 *
 * <p>Legs: four rigid tetrahedra that POINT DOWN. Each leg shares a
 * transverse hip edge with the body (front legs on (b1, b2), rear legs
 * on (b0, b3)) and adds an outrigger node and a foot node:
 * Tet(hip1, hip2, outrigger, foot). The foot is the downward apex, so
 * exactly 4 nodes -- the four feet -- contact the floor, like a table.
 * The shared edge is a hinge: the leg is a stiff paddle that swings
 * fore-aft about its hip edge but cannot fold or twist (all six leg
 * edges are passive struts).
 *
 * <p>Drive: two antagonistic CABLE muscles per leg (tension members --
 * they pull but never push). The protraction cable runs from a body
 * anchor ahead of the foot to the foot (swings the unloaded foot
 * forward and up, the pendulum arc clearing the ground); the
 * retraction cable runs from a body anchor behind the foot to the
 * foot -- during stance, with the foot planted, it hauls the body
 * forward over the foot: the power stroke. The cable pair also
 * brackets each leg at rest, holding the table legs from swinging. A
 * trot gait (diagonal legs in phase) walks the machine toward +x
 * (East).
 *
 * <p>Directional stability comes from a balanced compass bias, not from
 * the gait: the north-side leg nodes get an N nudge each step and the
 * south-side leg nodes an equal S nudge
 * ({@link HeadingStabilizerController}). Equal numbers N and S -- zero
 * E, zero W -- so the net bias is exactly zero: it is a stabilizer,
 * not a motor.
 *
 * <p>Node-node collisions are OFF: the model holds together through its
 * structure alone.
 *
 * <p>Parameters are public fields so the judge can sweep them.
 */
public final class CrawlerDemo {
  private CrawlerDemo() {
  }

  /** Body length (x, fore-aft), in pixels. */
  public static int body_length_px = 100;
  /** Body width (z, lateral), in pixels. */
  public static int body_width_px = 60;
  /** Ridge height above the body plate, in pixels. */
  public static int ridge_height_px = 55;
  /** Hip height above the ground, in pixels. */
  public static int hip_height_px = 55;
  /** Outrigger rise above the body plate, in pixels. */
  public static int outrigger_rise_px = 30;
  /** Outrigger lateral splay beyond the body half-width, in pixels. */
  public static int outrigger_splay_px = 25;
  /** Foot inset from its hip edge toward the body's middle, in pixels. */
  public static int foot_inset_px = 25;
  /**
   * Outrigger fore-aft offset from its hip edge, in pixels: the
   * outrigger sits this far on the OPPOSITE side of the hip edge from
   * the foot (front legs: outrigger ahead, foot behind; rear legs the
   * reverse). Equal and opposite, so each leg's centre of mass sits
   * directly under its hinge -- a balanced pendulum with no static
   * gravity torque trying to swing it.
   */
  public static int outrigger_offset_px = 25;
  /** Foot lateral splay beyond the body half-width, in pixels.
   * Wide stance for a stable support polygon. */
  public static int foot_splay_px = 20;
  /** Elasticity of the stiff body-skeleton struts. */
  public static int body_elasticity = 70;
  /** Elasticity of the leg springs (stays in the non-resonant band). */
  public static int leg_elasticity = 35;
  /** Muscle amplitude, 0-100%. */
  public static int muscle_amplitude_pct = 6;
  /**
   * Tim's "not tipping over" rule: element indices of the dorsal "top"
   * node (ridge T) and the "bottom" reference node (base b0). The top
   * must stay above the bottom for the whole run.
   */
  public static final int posture_top_index = 4;
  public static final int posture_bottom_index = 0;
  /** Min top-above-bottom separation (px) for the tip-over rule. */
  public static int posture_min_separation_px = 15;
  /** Muscle period, in ticks. */
  public static int muscle_period_ticks = 120;
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
   * When false, skip the cable muscles (struts and brace only).
   * Used to isolate the passive strut structure.
   */
  public static boolean build_cables = true;

  /**
   * When true, add a strut from each leg's outrigger to the ridge T.
   * This triangulates the leg's hinge (the outrigger can no longer
   * swing about the hip edge), making the leg a rigid extension of
   * the body. Without it the hinge is a free pendulum and the table
   * folds.
   */
  public static boolean brace_hinge = true;

  /**
   * The model's intended direction of travel on the floor plane.
   * The crawler walks toward +x: East.
   */
  public static CompassPoint compass_heading = CompassPoint.E;
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
   * Builds the crawler with its rear at x_px and its feet on the
   * ground. Returns the ridge node for tracking.
   */
  public static Node buildAt(int x_px) {
    final NodeManager node_manager = ContextManager.getNodeManager();
    node_manager.initial_reset();
    final LinkManager link_manager = node_manager.getLinkManager();
    link_manager.reset();
    node_manager.getFaceManager().reset();

    final Clazz clazz = node_manager.clazz_factory.getNew(0xFFFFFFFF);
    final NodeType node_type = node_manager.node_type_factory.getNew();
    final LinkType body_type =
        link_manager.link_type_factory.getNew(1, body_elasticity);
    final LinkType leg_type =
        link_manager.link_type_factory.getNew(1, leg_elasticity);

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

    // Layout: x grows toward the direction of travel (+x = East),
    // y grows downward, z grows southward (N = -z, S = +z).
    // The whole build is offset clear of the x = 0 and z = 0 walls so
    // Node.boundaryCheck() never clamps a built node (radius 18). The
    // rear outriggers sit outrigger_offset_px behind x0, so x0 starts
    // well clear.
    final int x0 = (x_px + 70) << Coords.shift;
    final int zo = 85 << Coords.shift;
    // Feet rest ~2px above the true wall; Grounding.restOnGround()
    // settles the whole model onto the floor at the end of the build.
    final int ground = (Coords.y_pixels << Coords.shift) - (2 << Coords.shift);

    final int L = body_length_px << Coords.shift;
    final int W = body_width_px << Coords.shift;
    final int H = ridge_height_px << Coords.shift;
    final int plate_y = ground - (hip_height_px << Coords.shift);

    // Body: two tetrahedral blocks sharing the face (b1, b3, T).
    // Tet A = (b0, b1, b3, T), Tet B = (b1, b2, b3, T).
    // b0 rear-north, b1 front-north, b2 front-south, b3 rear-south.
    final Node b0 = addNode(node_manager, clazz, node_type, x0, plate_y, zo - W / 2);
    final Node b1 = addNode(node_manager, clazz, node_type, x0 + L, plate_y, zo - W / 2);
    final Node b2 = addNode(node_manager, clazz, node_type, x0 + L, plate_y, zo + W / 2);
    final Node b3 = addNode(node_manager, clazz, node_type, x0, plate_y, zo + W / 2);
    final Node T = addNode(node_manager, clazz, node_type,
        x0 + L / 2, plate_y - H, zo);

    // Tet A edges: b0-b1, b0-b3, b0-T, b1-b3, b1-T, b3-T.
    strut(link_manager, body_type, clazz, b0, b1);
    strut(link_manager, body_type, clazz, b0, b3);
    strut(link_manager, body_type, clazz, b0, T);
    strut(link_manager, body_type, clazz, b1, b3);
    strut(link_manager, body_type, clazz, b1, T);
    strut(link_manager, body_type, clazz, b3, T);
    // Tet B edges: b1-b2, b1-b3, b1-T, b2-b3, b2-T, b3-T
    // (b1-b3, b1-T, b3-T already exist -- the shared face).
    strut(link_manager, body_type, clazz, b1, b2);
    strut(link_manager, body_type, clazz, b2, b3);
    final Link first_body_link =
        strut(link_manager, body_type, clazz, b2, T);

    // Legs: four downward-pointing tetrahedra. Each shares a transverse
    // hip edge with the body -- front legs on (b1, b2), rear legs on
    // (b0, b3) -- and adds an outrigger node (up and out, spreading the
    // top triangle for a stable stance) and a foot node (the downward
    // apex). All six leg edges are passive struts; the shared edge is
    // a hinge the stiff paddle swings about.
    //
    // Drive: two antagonistic CABLE muscles per leg. The protraction
    // cable (body anchor ahead of the foot -> foot) swings the unloaded
    // foot forward and up along its pendulum arc; the retraction cable
    // (body anchor behind the foot -> foot) is the stance power stroke
    // -- with the foot planted, shortening it hauls the body forward
    // over the foot. At rest the pair brackets the leg, holding the
    // table legs from swinging.
    final Node[][] hip_edges = {{b1, b2}, {b1, b2}, {b0, b3}, {b0, b3}};
    // Protraction anchor: ahead (+x) of the foot. Retraction anchor:
    // behind (-x) of the foot.
    final Node[] protract_anchors = {b1, b2, T, T};
    final Node[] retract_anchors = {T, T, b0, b3};
    final int[] sides = {-1, 1, -1, 1}; // outrigger/foot splay (z): N, S, N, S
    final String[] names = {"FL", "FR", "BL", "BR"};
    final Node[] north_leg_nodes = new Node[4];
    final Node[] south_leg_nodes = new Node[4];
    int north_count = 0;
    int south_count = 0;
    last_bias_layout = new LinkedHashMap<String, CompassPoint>();
    final int period = muscle_period_ticks;
    final int rise = outrigger_rise_px << Coords.shift;
    final int osplay = outrigger_splay_px << Coords.shift;
    final int inset = foot_inset_px << Coords.shift;
    final int ooffset = outrigger_offset_px << Coords.shift;
    final int fsplay = foot_splay_px << Coords.shift;
    for (int leg = 0; leg < 4; leg++) {
      final Node hip1 = hip_edges[leg][0];
      final Node hip2 = hip_edges[leg][1];
      final int side = sides[leg];
      final boolean front = leg < 2;
      final int leg_phase = ((leg_phases[leg] % period) + period) % period;
      // Protraction peaks mid-swing, retraction mid-stance.
      final int protract_phase = leg_phase;
      final int retract_phase = (leg_phase + period / 2) % period;

      final int edge_x = (hip1.pos.x + hip2.pos.x) / 2;
      // Outrigger: above the plate, splayed outward, and offset
      // fore-aft OPPOSITE the foot -- the leg's mass balances about
      // the hinge (no static gravity torque).
      final int out_x = front ? edge_x + ooffset : edge_x - ooffset;
      final Node outrigger = addNode(node_manager, clazz, node_type,
          out_x,
          plate_y - rise,
          zo + side * (W / 2 + osplay));
      // Foot: the downward apex -- inset toward the body's middle so a
      // body anchor sits ahead of it (protraction) and one behind it
      // (retraction), splayed just outside the body for stance width.
      final int foot_x = front ? edge_x - inset : edge_x + inset;
      final Node foot = addNode(node_manager, clazz, node_type,
          foot_x,
          ground,
          zo + side * (W / 2 + fsplay));

      // Rigid downward tetrahedron (hip1, hip2, outrigger, foot):
      // hip1-hip2 is the body edge (already exists); the other five
      // are passive struts.
      strut(link_manager, leg_type, clazz, hip1, outrigger);
      strut(link_manager, leg_type, clazz, hip2, outrigger);
      strut(link_manager, leg_type, clazz, hip1, foot);
      strut(link_manager, leg_type, clazz, hip2, foot);
      strut(link_manager, leg_type, clazz, outrigger, foot);
      // Antagonistic cable pair (skipped when build_cables is false --
      // passive strut-structure proof).
      if (build_cables) {
        cableMuscle(link_manager, leg_type, clazz,
            protract_anchors[leg], foot, protract_phase);
        cableMuscle(link_manager, leg_type, clazz,
            retract_anchors[leg], foot, retract_phase);
      }

      // Hinge brace: strut from the outrigger to the ridge T. This
      // locks the leg's hinge (the leg becomes a rigid extension of
      // the body). The leg still attaches via the shared hip edge;
      // the brace just prevents the free-pendulum fold.
      if (brace_hinge) {
        strut(link_manager, leg_type, clazz, outrigger, T);
      }

      // Compass-bias layout: north-side leg nodes get N, south-side
      // get S. Recorded for the judge; the net bias is zero.
      if (side < 0) {
        north_leg_nodes[north_count++] = outrigger;
        north_leg_nodes[north_count++] = foot;
        last_bias_layout.put(names[leg] + "-outrigger", CompassPoint.N);
        last_bias_layout.put(names[leg] + "-foot", CompassPoint.N);
      } else {
        south_leg_nodes[south_count++] = outrigger;
        south_leg_nodes[south_count++] = foot;
        last_bias_layout.put(names[leg] + "-outrigger", CompassPoint.S);
        last_bias_layout.put(names[leg] + "-foot", CompassPoint.S);
      }
    }

    // Heading stabilizer: N bias on the north leg nodes, S bias on
    // the south leg nodes. One instance on one passive link, so the
    // bias applies exactly once per dynamics step. Skipped for the
    // pure strut-structure proof.
    if (build_cables) {
      first_body_link.controller = new HeadingStabilizerController(
          trim(north_leg_nodes, north_count), trim(south_leg_nodes, south_count),
          heading_stabilizer_bias);
    }

    // No mid-air starts: rest the whole model on the ground plane.
    Grounding.restOnGround(node_manager);

    // Return the ridge node for tracking.
    return T;
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
   * simply goes slack. Built with a slight pre-tension (rest length a
   * few percent short of the actual distance) so the antagonistic
   * pair brackets the foot with positive stiffness -- this is what
   * locks the leg's hinge in the passive structure. The pair pulls
   * equally fore and aft, so the net force on the foot is zero.
   */
  public static int cable_pretension_pct = 100;
  private static void cableMuscle(LinkManager lm, LinkType template, Clazz clazz,
      Node a, Node b, int phase) {
    final int dist = distance(a, b);
    final int rest = (dist * cable_pretension_pct) / 100;
    final LinkType type = lm.link_type_factory.getNew(rest, template.elasticity);
    type.damping = template.damping;
    type.compression = false; // cable: no push when shorter than rest
    type.tension = true; // cable: pulls when longer than rest
    final Link link = lm.setLink(a, b, type, clazz);
    link.adjusted_rest_length = rest;
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
   * out to -2 * bias * width * sin(theta), i.e. yaw-restoring.
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
