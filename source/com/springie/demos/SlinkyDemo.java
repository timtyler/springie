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
import com.springie.muscles.Controller;
import com.springie.muscles.Muscles;
import com.springie.render.Coords;
import com.springie.world.World;

/**
 * A slinky: three octagonal wheels side by side that advance by discrete
 * end-over-end steps in a straight line, one step after another.
 *
 * <p>Structure (Tim's rules: tetrahedra, muscles on cables only):
 * each turn is a rigid passive ring -- the octagonal rim plus skip-one
 * diagonal chords -- and the gaps between turns are triangulated (axial
 * links plus one diagonal per segment), so the coil holds its shape
 * through structure alone. The three hubs stay independent (no rigid
 * axle: that inverts the drive).
 *
 * <p>Drive: the hub-to-rim spokes are cable muscles (tension-only) with
 * a pull-oriented ground-contact reflex ({@link SlinkyPullController}).
 * A cable cannot push, so only the front-stance pull is used: when a
 * spoke plants ahead of its hub, it shortens and drags the hub forward.
 * Muscle power only: no start kick.
 */
public final class SlinkyDemo {
  private SlinkyDemo() {
  }

  public static final int TURNS = 3;
  public static final int RIM_COUNT = 8;
  public static int rim_radius_px = 60;
  public static int rim_spacing_px = 50;
  public static int z_offset_px = 20;
  public static int rim_elasticity = 30;
  public static int spoke_elasticity = 25;
  public static int reflex_pull_pct = 30;
  /**
   * Ground friction, 0-100.
   *
   * Retuned 2026-09-18 after the symmetric-descale physics fix: at 100 the
   * coil's lateral contacts grab and yaw it sideways (straightness 0.30);
   * at 50 each end-over-end flip lands cleanly and it tracks dead straight
   * (dist 109px, straightness 0.99, 11 steps, strain 0.237).
   */
  public static int friction = 50;
  public static int settle_ticks = 60;

  public static Node[] coil_nodes = new Node[0];
  public static Node reference_node = null;

  public static void buildAt(int x_px) {
    final NodeManager node_manager = ContextManager.getNodeManager();
    node_manager.initial_reset();
    final LinkManager link_manager = node_manager.getLinkManager();
    link_manager.reset();
    node_manager.getFaceManager().reset();

    final Clazz clazz = node_manager.clazz_factory.getNew(0xFFFFFFFF);
    final NodeType node_type = node_manager.node_type_factory.getNew();

    Muscles.enabled = true;
    World.gravity_active = true;
    World.gravity_strength = 5;
    World.ground_friction = friction;
    World.global_temperature = 0;
    com.springie.elements.nodes.Node.viscocity = 2;

    final int ground = Coords.y_pixels << Coords.shift;
    final int cx = x_px << Coords.shift;
    final int cy = ground - (rim_radius_px << Coords.shift);
    final int radius = rim_radius_px << Coords.shift;
    final int z0 = z_offset_px << Coords.shift;
    final int dz = rim_spacing_px << Coords.shift;

    // Rims: node i at body angle 2*PI*i / RIM_COUNT (vertex down).
    final Node[][] rims = new Node[TURNS][RIM_COUNT];
    for (int k = 0; k < TURNS; k++) {
      for (int i = 0; i < RIM_COUNT; i++) {
        final double a = 2.0 * Math.PI * i / RIM_COUNT;
        final int x = cx + (int) (radius * Math.cos(a));
        final int y = cy + (int) (radius * Math.sin(a));
        final int z = z0 + k * dz;
        rims[k][i] = addNode(node_manager, clazz, node_type, x, y, z);
      }
    }
    // Three independent hubs (one per turn), NOT connected by an axle.
    // Each turn is a wheel; the inter-rim bracing keeps them aligned.
    // A rigid axle locks the hubs and inverts the drive direction.
    final Node[] hubs = new Node[TURNS];
    for (int k = 0; k < TURNS; k++) {
      hubs[k] = addNode(node_manager, clazz, node_type,
          cx, cy, z0 + k * dz);
    }

    // Per-turn rim edges (octagon perimeter).
    for (int k = 0; k < TURNS; k++) {
      for (int i = 0; i < RIM_COUNT; i++) {
        final int j = (i + 1) % RIM_COUNT;
        passive(link_manager, clazz, rims[k][i], rims[k][j], rim_elasticity);
      }
    }

    // Skip-one diagonal chords brace each ring in-plane (alternating,
    // like the wheel): node i to node i+2 for even i. Together with the
    // rim edges they triangulate the octagon so the ring cannot shear.
    for (int k = 0; k < TURNS; k++) {
      for (int i = 0; i < RIM_COUNT; i += 2) {
        final int j = (i + 2) % RIM_COUNT;
        passive(link_manager, clazz, rims[k][i], rims[k][j], rim_elasticity);
      }
    }

    // Inter-turn bracing: axial links plus one diagonal per segment per
    // gap. The diagonal (rims[k][i] to rims[k+1][i+1]) triangulates each
    // quad of the gap surface, so the turns cannot shear or fold
    // relative to each other -- the coil is a rigid 3D truss.
    for (int k = 0; k < TURNS - 1; k++) {
      for (int i = 0; i < RIM_COUNT; i++) {
        passive(link_manager, clazz, rims[k][i], rims[k + 1][i],
            rim_elasticity);
        final int j = (i + 1) % RIM_COUNT;
        passive(link_manager, clazz, rims[k][i], rims[k + 1][j],
            rim_elasticity);
      }
    }

    final java.util.ArrayList<Node> all = new java.util.ArrayList<>();
    for (int k = 0; k < TURNS; k++) {
      for (int i = 0; i < RIM_COUNT; i++) {
        all.add(rims[k][i]);
      }
    }
    for (int k = 0; k < TURNS; k++) {
      all.add(hubs[k]);
    }
    coil_nodes = all.toArray(new Node[0]);
    reference_node = rims[TURNS / 2][0];

    // Hub spokes: each turn drives itself. The spokes are cable
    // muscles (tension-only, Tim's muscle rule) with a pull-oriented
    // reflex: a cable cannot push, so the back-stance push-off is
    // dropped and only the front-stance pull drives.
    for (int k = 0; k < TURNS; k++) {
      for (int i = 0; i < RIM_COUNT; i++) {
        pullSpoke(link_manager, clazz, hubs[k], rims[k][i], ground);
      }
    }

    for (int t = 0; t < settle_ticks; t++) {
      node_manager.nodeAndLinkUpdate();
    }
  }

  public static void build() {
    buildAt(400);
  }

  private static Node addNode(NodeManager node_manager, Clazz clazz,
      NodeType node_type, int x, int y, int z) {
    return node_manager.addNewAgent(new Point3D(x, y, z), clazz, node_type);
  }

  private static void passive(LinkManager link_manager, Clazz clazz,
      Node n1, Node n2, int elasticity) {
    final LinkType type = link_manager.link_type_factory.getNew(
        distance(n1, n2), elasticity);
    final Link link = link_manager.setLink(n1, n2, type, clazz);
    link.adjusted_rest_length = type.length;
  }

  private static void pullSpoke(LinkManager link_manager, Clazz clazz,
      Node hub, Node rim, int ground_y) {
    final int base = distance(hub, rim);
    final LinkType type = link_manager.link_type_factory.getNew(
        base, spoke_elasticity);
    // Muscle on a cable: tension-only, never pushes.
    type.compression = false;
    type.tension = true;
    final Link link = link_manager.setLink(hub, rim, type, clazz);
    link.adjusted_rest_length = base;
    link.controller =
        new SlinkyPullController(hub, rim, base, reflex_pull_pct, ground_y, 1);
  }

  private static int distance(Node a, Node b) {
    final int dx = a.pos.x - b.pos.x;
    final int dy = a.pos.y - b.pos.y;
    final int dz = a.pos.z - b.pos.z;
    return (int) Math.sqrt((long) dx * dx + (long) dy * dy + (long) dz * dz);
  }

  /**
   * A ground-contact pull reflex for cable spoke muscles.
   *
   * <p>Unlike {@link WheelPushController} (which extends a strut to push
   * off), a cable can only pull: when the spoke's rim node is on the
   * ground ahead of the hub (front stance, in the rolling direction),
   * the spoke shortens, dragging the hub forward. At all other times it
   * holds its base rest length. Self-synchronizing: the ground contact
   * sets the timing.
   */
  static final class SlinkyPullController implements Controller {
    private final Node hub;
    private final Node rim;
    private final int base_length;
    private final int pull_length;
    private final int ground_y;
    private final int direction;

    /**
     * @param hub the hub node
     * @param rim the rim node of this spoke
     * @param base_length the spoke's natural rest length (internal units)
     * @param pull_pct percent to contract when pulling (front stance)
     * @param ground_y the ground level in internal units
     * @param direction +1 to roll toward +X, -1 toward -X
     */
    SlinkyPullController(Node hub, Node rim, int base_length,
        int pull_pct, int ground_y, int direction) {
      this.hub = hub;
      this.rim = rim;
      this.base_length = base_length;
      this.pull_length = base_length - (base_length * pull_pct / 100);
      this.ground_y = ground_y;
      this.direction = direction;
    }

    @Override
    public void update(Link link, long tick) {
      // Near the ground? (within 8px above it)
      final boolean on_ground =
          rim.pos.y >= ground_y - (8 << Coords.shift);
      if (!on_ground) {
        link.adjusted_rest_length = base_length;
        return;
      }
      // In front of the hub (in the rolling direction)?
      final int dx = rim.pos.x - hub.pos.x;
      final int threshold = 4 << Coords.shift;
      final boolean front =
          direction > 0 ? dx > threshold : dx < -threshold;
      // Front stance: contract, pull the hub forward.
      // (A cable cannot push; there is no back-stance action.)
      link.adjusted_rest_length = front ? pull_length : base_length;
    }
  }
}
