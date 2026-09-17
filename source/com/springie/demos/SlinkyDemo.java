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
import com.springie.muscles.Muscles;
import com.springie.render.Coords;
import com.springie.world.World;

/**
 * A slinky: three octagonal wheels side by side that advance by discrete
 * end-over-end steps in a straight line, one step after another.
 *
 * <p>Geometry follows the wheel demo: RIM_COUNT nodes per rim at body
 * angles 2*PI*i/RIM_COUNT (vertex down), rim_radius_px radius, each rim
 * with its own hub at its centre. Adjacent rims are joined by axial
 * links; there is no rigid axle between the hubs (that inverts the
 * drive). The rims are spaced widely along z for sideways stability.
 *
 * <p>Drive: the hub-to-rim spokes are muscles with the wheel's proven
 * ground-contact push-off reflex ({@link WheelPushController}). Each
 * turn steps on its own; the axial links keep them aligned without
 * locking them into a fight. The octagonal rims make the motion
 * discretely stepped (45 degrees per face) rather than a smooth roll.
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
  public static int reflex_push_pct = 35;
  public static int reflex_pull_pct = 5;
  public static int friction = 100;
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

    // Axial links keep the turns together. The full tetrahedral
    // bracing locks them too rigidly; axial links alone let each turn
    // step while staying aligned.
    for (int k = 0; k < TURNS - 1; k++) {
      for (int i = 0; i < RIM_COUNT; i++) {
        passive(link_manager, clazz, rims[k][i], rims[k + 1][i],
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

    // Hub spokes: each turn drives itself.
    for (int k = 0; k < TURNS; k++) {
      for (int i = 0; i < RIM_COUNT; i++) {
        reflexSpoke(link_manager, clazz, hubs[k], rims[k][i], ground);
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

  private static void reflexSpoke(LinkManager link_manager, Clazz clazz,
      Node hub, Node rim, int ground_y) {
    final LinkType type = link_manager.link_type_factory.getNew(
        distance(hub, rim), spoke_elasticity);
    final Link link = link_manager.setLink(hub, rim, type, clazz);
    final int base = distance(hub, rim);
    link.adjusted_rest_length = base;
    link.controller = new WheelPushController(
        hub, rim, base, reflex_push_pct, reflex_pull_pct, ground_y, 1);
  }

  private static int distance(Node a, Node b) {
    final int dx = a.pos.x - b.pos.x;
    final int dy = a.pos.y - b.pos.y;
    final int dz = a.pos.z - b.pos.z;
    return (int) Math.sqrt((long) dx * dx + (long) dy * dy + (long) dz * dz);
  }
}
