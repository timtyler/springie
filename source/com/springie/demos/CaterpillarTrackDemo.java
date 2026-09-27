// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.context.ContextManager;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.links.LinkManager;
import com.springie.elements.links.LinkType;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.elements.nodes.NodeType;
import com.springie.geometry.Point3D;
import com.springie.render.Coords;
import com.springie.world.Grounding;
import com.springie.world.World;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Caterpillar track (Tim, 2026-09-26): starting over from scratch.
 *
 * <p>Three concentric rings with the same center, 21 nodes per ring.
 * Two-thirds of the nodes are deleted per the patterns (X=delete,
 * O=keep):
 * <ul>
 * <li>Ring 0 (inner, r=120, z=0): XXO -- keep i%3==2</li>
 * <li>Ring 1 (outer, r=140, z=+30): XOX -- keep i%3==1</li>
 * <li>Ring 2 (outer, r=140, z=-30): OXX -- keep i%3==0</li>
 * </ul>
 * 7 nodes per ring (21 total), staggered so they interleave.
 *
 * <p>Each node is linked to the two nearest nodes on each of the other
 * two circles (struts, deduplicated).
 */
public final class CaterpillarTrackDemo {
  private CaterpillarTrackDemo() {
  }

  /** Nodes per ring before deletion. */
  public static final int NODES_PER_RING = 21;

  /** Inner ring radius, in pixels. */
  public static final int INNER_RADIUS_PX = 120;

  /** Outer ring radius, in pixels. */
  public static final int OUTER_RADIUS_PX = 140;

  /** Outer ring z offset (half the track width), in pixels. */
  public static final int HALF_WIDTH_PX = 30;

  /** Node size, in pixels (physical radius). */
  public static int node_size_px = 16;

  /** Ground friction. */
  public static int friction = 100;

  /** Log mass for all nodes. */
  public static int log_mass = 0;

  /** Link elasticity. */
  public static int elasticity = 64;

  /** Returns true if the node at index i in the given ring is kept. */
  private static boolean keep(int ring, int i) {
    final int mod = i % 3;
    // Ring 0: XXO, Ring 1: XOX, Ring 2: OXX (X=delete, O=keep).
    return (ring == 0 && mod == 2)
        || (ring == 1 && mod == 1)
        || (ring == 2 && mod == 0);
  }

  /** Squared 3D distance between two nodes (descaled). */
  private static long dist2(Node a, Node b) {
    final long dx = (long) (a.pos.x - b.pos.x) >> Coords.shift;
    final long dy = (long) (a.pos.y - b.pos.y) >> Coords.shift;
    final long dz = (long) (a.pos.z - b.pos.z) >> Coords.shift;
    return dx * dx + dy * dy + dz * dz;
  }

  /**
   * Builds the rings centred at the given x (pixels), resting on the
   * ground. Returns the first kept inner-ring node.
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

    World.gravity_active = true;
    World.gravity_strength = 2;
    World.ground_friction = friction;
    World.global_temperature = 0;

    // Ground is the high-Y wall (positive gravity pulls toward +Y).
    final int ground_y_px = Coords.y_pixels - node_size_px - 1;
    final int centre_y_px = ground_y_px - OUTER_RADIUS_PX;
    final int centre_y = centre_y_px << Coords.shift;
    final int cx = x_px << Coords.shift;
    final int half_width = HALF_WIDTH_PX << Coords.shift;

    final List<Node> ring0 = new ArrayList<>();
    final List<Node> ring1 = new ArrayList<>();
    final List<Node> ring2 = new ArrayList<>();

    for (int ring = 0; ring < 3; ring++) {
      final List<Node> kept = ring == 0 ? ring0 : ring == 1 ? ring1 : ring2;
      final int radius_px = ring == 0 ? INNER_RADIUS_PX : OUTER_RADIUS_PX;
      final int z = ring == 0 ? 0 : ring == 1 ? half_width : -half_width;
      for (int i = 0; i < NODES_PER_RING; i++) {
        if (!keep(ring, i)) {
          continue;
        }
        final double angle = 2.0 * Math.PI * i / NODES_PER_RING;
        final int dx = (int) (radius_px * Math.cos(angle)) << Coords.shift;
        final int dy = (int) (radius_px * Math.sin(angle)) << Coords.shift;
        kept.add(node_manager.addNewAgent(
            new Point3D(cx + dx, centre_y + dy, z), clazz, node_type));
      }
    }

    // Link every node to the two nearest nodes on each of the other
    // two circles. Deduplicated (no duplicate links).
    final Set<String> linked = new HashSet<>();
    final List<List<Node>> rings = List.of(ring0, ring1, ring2);
    for (int r = 0; r < 3; r++) {
      for (int s = 0; s < 3; s++) {
        if (s == r) {
          continue;
        }
        for (Node node : rings.get(r)) {
          // Find the two nearest nodes on ring s.
          Node best1 = null;
          Node best2 = null;
          long d1 = Long.MAX_VALUE;
          long d2 = Long.MAX_VALUE;
          for (Node other : rings.get(s)) {
            final long d = dist2(node, other);
            if (d < d1) {
              d2 = d1;
              best2 = best1;
              d1 = d;
              best1 = other;
            } else if (d < d2) {
              d2 = d;
              best2 = other;
            }
          }
          for (Node other : new Node[] {best1, best2}) {
            if (other == null) {
              continue;
            }
            // Deduplicate: canonical key from identity hashes.
            final int h1 = System.identityHashCode(node);
            final int h2 = System.identityHashCode(other);
            final String key =
                Math.min(h1, h2) + "-" + Math.max(h1, h2);
            if (linked.add(key)) {
              final int length =
                  (int) Math.sqrt(dist2(node, other)) << Coords.shift;
              final LinkType type = link_manager.link_type_factory.getNew(
                  length, elasticity);
              // Struts everywhere for this model (Tim).
              link_manager.setLink(node, other, type, clazz);
            }
          }
        }
      }
    }

    // Rest on the ground (no mid-air start, per Tim's grounding rule).
    Grounding.restOnGround(node_manager);

    return ring0.get(0);
  }

  /** Builds the rings at the default x position. */
  public static Node build() {
    return buildAt(300);
  }
}
