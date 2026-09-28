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
import com.springie.muscles.Oscillator;
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
 * <li>Ring 0 (inner, r=60, z=0): XXO -- keep i%3==2</li>
 * <li>Ring 1 (outer, r=140, z=+46): XOX -- keep i%3==1</li>
 * <li>Ring 2 (outer, r=140, z=-46): OXX -- keep i%3==0</li>
 * </ul>
 * 7 nodes per ring (21 total), staggered so they interleave.
 *
 * <p>The (radius, z) positions of the three rings form an equilateral
 * triangle: (60, 0), (140, +46), (140, -46) -- sides 92.3, 92.3, 92 --
 * so a slice through the donut has a triangular cross-section (Tim).
 *
 * <p>Each node is linked to the two nearest nodes on each of the other
 * two circles (struts, deduplicated). Node radius and link radius are
 * equal (Tim): the track is a uniform tube.
 *
 * <p>Axle (Tim, 2026-09-27): two nodes on the z-axis through the middle
 * of the track, joined by a rigid shaft and spoked to every inner ring
 * node on both sides. The ends carry opposite compass headings (N on
 * the -z end, S on the +z end), so the universe compass bias pulls them
 * apart and tensions the spokes -- stabilising the track against
 * tipping over. The whole model is offset +z (Z_OFFSET_PX): the depth
 * wall at z=0 would otherwise crush the -z ring and axle end flat
 * against it on the first tick.
 */
public final class CaterpillarTrackDemo {
  private CaterpillarTrackDemo() {
  }

  /** Nodes per ring before deletion. */
  public static final int NODES_PER_RING = 21;

  /** Inner ring radius, in pixels. Halved from 120 (Tim, 2026-09-27). */
  public static final int INNER_RADIUS_PX = 60;

  /** Outer ring radius, in pixels. */
  public static final int OUTER_RADIUS_PX = 140;

  /**
   * Outer ring z offset (half the track width), in pixels. With inner
   * radius 60 and outer radius 140, half-width 80/sqrt(3) makes the
   * (radius, z) cross-section an equilateral triangle.
   */
  public static final int HALF_WIDTH_PX = 46;

  /**
   * Axle half-length, in pixels. The axle runs along the z-axis through
   * the middle of the track; its ends sit outside the outer rings so the
   * spokes angle outward like a bicycle wheel.
   */
  public static final int AXLE_HALF_PX = 92;

  /**
   * Z offset of the track middle, in pixels. The depth wall at z=0 (with
   * the 16px node radius, nothing may sit below z=16) would otherwise
   * crush the -z ring and the -z axle end flat against it on the first
   * tick -- measured 2026-09-27. The offset puts the rearmost point (the
   * -z axle end) at 32px, clear of the wall with margin.
   */
  public static final int Z_OFFSET_PX = 124;

  /** Node size, in pixels (physical radius). */
  public static int node_size_px = 16;

  /**
   * Node/link radius in fixed-point units. Radii are stored in
   * fixed-point (pixels &lt;&lt; Coords.shift); node radius and link
   * radius are equal (Tim).
   */
  private static int nodeRadius() {
    return node_size_px << Coords.shift;
  }

  /** Lift the model above the canvas bottom so it renders fully in view. */
  public static final int VIEW_MARGIN_PX = 60;

  /** Ground friction. */
  public static int friction = 100;

  /**
   * Default compass pull on the axle ends, in velocity units per frame
   * (same units as the gravity strength). N on the -z end and S on the
   * +z end, so the bias pulls the ends apart and tensions the spokes.
   * Tunable live via the Compass bias slider (Universe tab); 0 disables.
   * Set to 40 on Tim's order (2026-09-28).
   */
  public static int compass_bias = 40;

  /** Log mass for all nodes. Was 0 (mass=1): far too light for the
      spring stiffness, causing numerical divergence. 15 matches
      HamsterWheel rim. */
  public static int log_mass = 15;

  /** Link elasticity. */
  public static int elasticity = 20;

  /**
   * Muscle pulse depth on the central-circle links, as a percent of the
   * rest length: the links vary in length about their rest length (Tim).
   */
  public static int muscle_amplitude_pct = 20;

  /**
   * Muscle oscillator period, in ticks. The central-circle links pulse
   * as a travelling wave; tuned so the wave completes 2 turns for every
   * turn of the wheel (Tim: f = 2).
   */
  public static int muscle_period_ticks = 480;

  /** Returns true if the node at index i in the given ring is kept. */
  private static boolean keep(int ring, int i) {
    final int mod = i % 3;
    // Ring 0: XXO, Ring 1: XOX, Ring 2: OXX (X=delete, O=keep).
    return (ring == 0 && mod == 2)
        || (ring == 1 && mod == 1)
        || (ring == 2 && mod == 0);
  }

  /** Squared 3D distance between two nodes (descaled, for comparisons). */
  private static long dist2(Node a, Node b) {
    final long dx = (long) (a.pos.x - b.pos.x) >> Coords.shift;
    final long dy = (long) (a.pos.y - b.pos.y) >> Coords.shift;
    final long dz = (long) (a.pos.z - b.pos.z) >> Coords.shift;
    return dx * dx + dy * dy + dz * dz;
  }

  /**
   * Exact link length in fixed-point units, matching the actual distance
   * between the nodes (no integer-pixel truncation).
   */
  private static int exactLength(Node a, Node b) {
    final double scale = 1 << Coords.shift;
    final double dx = (a.pos.x - b.pos.x) / scale;
    final double dy = (a.pos.y - b.pos.y) / scale;
    final double dz = (a.pos.z - b.pos.z) / scale;
    final double dist_px = Math.sqrt(dx * dx + dy * dy + dz * dz);
    return (int) Math.round(dist_px * scale);
  }

  /**
   * Adds a strut link between two nodes (deduplicated), with exact rest
   * length and the track's uniform node/link radius. Struts everywhere
   * for this model (Tim).
   */
  private static void addStrut(LinkManager link_manager, Clazz clazz,
      Set<String> linked, Node a, Node b) {
    final int h1 = System.identityHashCode(a);
    final int h2 = System.identityHashCode(b);
    final String key = Math.min(h1, h2) + "-" + Math.max(h1, h2);
    if (linked.add(key)) {
      final int length = exactLength(a, b);
      final LinkType type =
          link_manager.link_type_factory.getNew(length, elasticity);
      // Link radius equals the node radius (Tim).
      type.radius = nodeRadius();
      link_manager.setLink(a, b, type, clazz);
    }
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
    node_type.setSize(nodeRadius());

    World.gravity_active = true;
    World.gravity_strength = 2;
    World.ground_friction = friction;
    World.global_temperature = 0;

    // The muscle wave runs on oscillator slot 0 (Tim): the central
    // circle's links vary in length about their rest length.
    Muscles.enabled = true;
    Muscles.active_oscillator = 0;
    Muscles.activeOscillator().setAmplitude(
        muscle_amplitude_pct * Muscles.UNITY / 100);
    Muscles.activeOscillator().setPeriodTicks(muscle_period_ticks);
    Muscles.activeOscillator().setPhase(0);

    // Ground is the high-Y wall (positive gravity pulls toward +Y).
    // Lift by VIEW_MARGIN_PX so the track renders fully in view.
    final int ground_y_px = Coords.y_pixels - node_size_px - 1;
    final int centre_y_px = ground_y_px - OUTER_RADIUS_PX - VIEW_MARGIN_PX;
    final int centre_y = centre_y_px << Coords.shift;
    final int cx = x_px << Coords.shift;
    final int half_width = HALF_WIDTH_PX << Coords.shift;
    // The whole model sits forward of the z=0 depth wall (see Z_OFFSET_PX).
    final int z_offset = Z_OFFSET_PX << Coords.shift;

    final List<Node> ring0 = new ArrayList<>();
    final List<Node> ring1 = new ArrayList<>();
    final List<Node> ring2 = new ArrayList<>();

    for (int ring = 0; ring < 3; ring++) {
      final List<Node> kept = ring == 0 ? ring0 : ring == 1 ? ring1 : ring2;
      final int radius_px = ring == 0 ? INNER_RADIUS_PX : OUTER_RADIUS_PX;
      final int z = z_offset + (ring == 0 ? 0 : ring == 1 ? half_width : -half_width);
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

    // Rest on the ground BEFORE linking. restOnGround runs boundaryCheck
    // which can micro-adjust nodes; linking afterwards keeps every link
    // length exactly matching its node distance.
    // Note: VIEW_MARGIN_PX is applied in the initial centre_y above, so
    // the model sits above the canvas bottom for full visibility (Tim).
    // restOnGround is skipped here to preserve that margin.

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
              final int length = exactLength(node, other);
              final LinkType type = link_manager.link_type_factory.getNew(
                  length, elasticity);
              // Link radius equals the node radius (Tim).
              type.radius = nodeRadius();
              // Struts everywhere for this model (Tim).
              link_manager.setLink(node, other, type, clazz);
            }
          }
        }
      }
    }

    // Link adjacent nodes within each circle.
    // The kept nodes are in angular order; link consecutive pairs
    // plus the wraparound, forming a closed ring per circle.
    // The central circle's links are muscles (Tim): each varies in
    // length about its rest length, phased as a travelling wave with
    // one full wavelength around the ring.
    final Oscillator oscillator = Muscles.activeOscillator();
    final int period = oscillator.getPeriodTicks();
    final GlobalOscillatorController muscle =
        new GlobalOscillatorController(Muscles.active_oscillator);
    for (int r = 0; r < rings.size(); r++) {
      final List<Node> ring = rings.get(r);
      final int n = ring.size();
      for (int i = 0; i < n; i++) {
        final Node a = ring.get(i);
        final Node b = ring.get((i + 1) % n);
        final int h1 = System.identityHashCode(a);
        final int h2 = System.identityHashCode(b);
        final String key = Math.min(h1, h2) + "-" + Math.max(h1, h2);
        if (linked.add(key)) {
          final int length = exactLength(a, b);
          final LinkType type =
              link_manager.link_type_factory.getNew(length, elasticity);
          // Link radius equals the node radius (Tim).
          type.radius = nodeRadius();
          // Struts everywhere for this model (Tim); the central
          // circle's links additionally carry the muscle wave.
          final Link link = link_manager.setLink(a, b, type, clazz);
          if (r == 0) {
            final int phase = i * period / n;
            link.phase = phase;
            link.controller = muscle;
            link.adjusted_rest_length = (int) (
                ((long) type.length * oscillator.getScale(0, phase))
                    >> Coords.shift);
          }
        }
      }
    }

    // Axle (Tim, 2026-09-27): through the middle of the track along the
    // z-axis, attached to the inner circle nodes on both sides. The two
    // ends are joined by a rigid shaft; each end is spoked to every
    // inner ring node.
    final int axle_half = AXLE_HALF_PX << Coords.shift;
    final Node axle_north = node_manager.addNewAgent(
        new Point3D(cx, centre_y, z_offset - axle_half), clazz, node_type);
    final Node axle_south = node_manager.addNewAgent(
        new Point3D(cx, centre_y, z_offset + axle_half), clazz, node_type);

    // Opposite compass headings: the universe compass bias pulls the -z
    // end toward N and the +z end toward S -- the ends are pulled apart,
    // tensioning the spokes and stabilising the track against tipping
    // over (same convention as the hamster wheel axle stabilizer).
    axle_north.compass = CompassPoint.N;
    axle_south.compass = CompassPoint.S;
    CompassPoint.bias_size = compass_bias;

    // Rigid shaft joining the ends into a single axle.
    addStrut(link_manager, clazz, linked, axle_north, axle_south);

    // Spokes: each axle end to every inner ring node.
    for (final Node end : new Node[] {axle_north, axle_south}) {
      for (final Node inner : ring0) {
        addStrut(link_manager, clazz, linked, end, inner);
      }
    }

    return ring0.get(0);
  }

  /** Builds the rings at the default x position. */
  public static Node build() {
    return buildAt(300);
  }
}
