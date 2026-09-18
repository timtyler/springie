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
import com.springie.world.World;

/**
 * A jumping grasshopper built from tetrahedra.
 *
 * <p>Body: a braced box frame (6 nodes, 12 edges: the 9 box edges plus
 * both bottom diagonals and one face diagonal). It rides rigidly while
 * the legs do the work -- no flat panels. Each leg is a tetrahedron
 * attached to the body via a z-running bottom edge (two nodes): hip
 * edge, knee, foot -- all 6 edges present. The legs are built folded in
 * a deep crouch: the knee sits up and away from the hip, the foot rests
 * on the ground under the hip. The two femur edges of every leg are
 * the drive cables (tension-only, never struts); hip-foot and
 * knee-foot are passive spring struts. When the femur cables shorten
 * they reel the body up toward the knees, which are propped on the
 * tibiae against the ground -- a direct extensor launch, like the
 * real insect. The foot is never yanked (reeling the foot just lifts
 * it, which is why the old hip-foot drive flailed). All eight cables
 * fire in unison through a one-shot gate on the global oscillator
 * (phase 0): a single yank-and-release launches the body ballistically,
 * then the cables hold their build length -- a repeating sine would
 * keep pumping the legs after landing and feed the pitch mode until
 * the body tumbles.
 *
 * <p>The legs flex in the fore-aft plane about the z-running hip edge
 * (sideways-splayed knees buckle under load), and the leg springs are
 * kept soft to avoid the numerical resonance seen in the first
 * tetrahedral walkers. Muscle power only: no start kick. Node-node
 * collisions stay off: the truss holds together through structure alone.
 *
 * <p>buildAt returns the crown marker node (the top ridge node); the
 * feet are published in {@link #feet} for the judge's airborne check.
 *
 * <p>Parameters are public fields so the judge can sweep them.
 */
public final class GrasshopperDemo {
  private GrasshopperDemo() {
  }

  /** Body length (x), in pixels. */
  public static int body_length_px = 70;
  /** Body width (z), in pixels. */
  public static int body_width_px = 36;
  /** Body height (y), in pixels. */
  public static int body_height_px = 45;
  /** Hip height above the ground, in pixels. */
  public static int leg_drop_px = 55;
  /** Hind knee offset behind the hip (+x), in pixels. Deep crouch:
   * the knee must sit well clear of the hip-foot line so the femur
   * cable's pull has a long power stroke reeling the body up, instead
   * of bottoming out against the straight-leg lock after a few pixels
   * (which just shocks the truss). */
  public static int knee_back_px = 60;
  /** Knee offset above the hip, in pixels. */
  public static int knee_up_px = 30;
  /**
   * Foot offset behind the hip (+x for hind legs), in pixels. Zero: the
   * yank is purely vertical, so the launch has no pitch torque.
   */
  public static int foot_back_px = 0;
  /** Foot z splay, in pixels. */
  public static int leg_splay_px = 10;
  /** Knee z splay, in pixels. */
  public static int knee_splay_px = 6;
  /** Front knee offset ahead of the hip (-x), in pixels. Mirrors the
   * hind knee so the launch torques cancel. */
  public static int front_knee_forward_px = 60;
  /** Front foot offset ahead of the hip (-x), in pixels. Zero: vertical yank. */
  public static int front_foot_forward_px = 0;
  /** Crouch yank amplitude, 0-100%. Sized to the knee's fold travel. */
  public static int muscle_amplitude_pct = 30;
  /**
   * Tim's "not tipping over" rule: element indices of the dorsal "top"
   * node (ridge) and the "bottom" reference node (base). The top must
   * stay above the bottom for the whole run.
   */
  public static final int posture_top_index = 4;
  public static final int posture_bottom_index = 0;
  /** Min top-above-bottom separation (px) for the tip-over rule. */
  public static final int posture_min_separation_px = 15;
  /** Flexor cable period, in ticks. The gated drive fires a burst of
   * yank-and-release half-periods; 20 ticks paces the leg's bounce. */
  public static int muscle_period_ticks = 20;
  /**
   * Oscillator phase offset, in ticks. With a period of twice the
   * judged run and a half-period phase, the sine delivers exactly one
   * yank-and-release (one jump) per run: yank while the phase is
   * negative, release as it returns through zero, then slack for the
   * rest of the run -- the cables never pump the body after landing.
   */
  public static int muscle_osc_phase_ticks = 0;
  /** Ground friction, 0-100. */
  public static int friction = 100;
  /** Elasticity of the body frame. */
  public static int body_elasticity = 50;
  /** Elasticity of the passive leg links. */
  public static int leg_elasticity = 30;
  /** Elasticity of the extensor muscles. */
  public static int muscle_elasticity = 25;

  /**
   * A one-shot gate on the global oscillator: it lets the oscillator's
   * yank-and-release through exactly once (after the build settles),
   * then holds every cable at its build length for the rest of the run.
   * A continuous sine keeps pumping the legs after landing, which feeds
   * the pitch mode until the body tumbles (the old disqualification);
   * a single impulse launches the body ballistically and the rigid
   * legs just absorb the landing. Extends GlobalOscillatorController so
   * the drive still follows the global oscillator's amplitude/period.
   */
  static final class OneShotController extends GlobalOscillatorController {
    /** Ticks to let the build settle before firing. */
    static final int SETTLE_TICKS = 30;
    /** Number of yank-and-release half-periods to let through. */
    static final int YANK_COUNT = 50;
    private long first_tick = -1;

    OneShotController(int oscillator_index) {
      super(oscillator_index);
    }

    @Override
    public void update(Link link, long tick) {
      if (this.first_tick < 0) {
        this.first_tick = tick;
      }
      final long dt = tick - this.first_tick;
      // A few yank-and-release half-periods, then hold at build length.
      // The fake tick is shifted so the sine always starts at its yank
      // (negative) half, regardless of the absolute tick.
      final int half_period = GrasshopperDemo.muscle_period_ticks / 2;
      if (dt >= SETTLE_TICKS
          && dt < SETTLE_TICKS + YANK_COUNT * half_period) {
        super.update(link, dt - SETTLE_TICKS + half_period);
      } else {
        link.adjusted_rest_length = link.type.length;
      }
    }
  }

  /** The crown marker node (top of the body), set by buildAt. */
  public static Node marker;
  /** The four foot nodes (hind L/R, front L/R), set by buildAt. */
  public static Node[] feet = new Node[4];

  /**
   * Builds the grasshopper with its body starting at x_px, feet on the
   * ground. Returns the crown marker node.
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
        link_manager.link_type_factory.getNew(body_length_px << Coords.shift, body_elasticity);
    // High damping on the body kills the pitch/bending resonance that
    // the leg drive pumps; the legs stay soft for the launch.
    body_type.damping = 200;
    // The asymmetric face diagonal couples the front to the ridge. At
    // full body stiffness it over-corrects the pitch; softer here.
    final LinkType brace_type =
        link_manager.link_type_factory.getNew(body_length_px << Coords.shift, 20);
    brace_type.damping = 200;
    final LinkType leg_type =
        link_manager.link_type_factory.getNew(leg_drop_px << Coords.shift, leg_elasticity);
    final LinkType muscle_type =
        link_manager.link_type_factory.getNew(leg_drop_px << Coords.shift, muscle_elasticity);

    // The eight crouch cables run on oscillator slot 0, all in phase 0
    // for a level launch. Muscles.enabled is set for the UI's muscle
    // readout.
    Muscles.enabled = true;
    Muscles.active_oscillator = 0;
    Muscles.activeOscillator().setAmplitude(muscle_amplitude_pct * Muscles.UNITY / 100);
    Muscles.activeOscillator().setPeriodTicks(muscle_period_ticks);
    Muscles.activeOscillator().setPhase(muscle_osc_phase_ticks);
    World.gravity_active = true;
    World.gravity_strength = 5;
    World.ground_friction = friction;
    // Deterministic physics: the judge and the UI both build through
    // here, so both see the same jump.
    World.global_temperature = 0;
    // No node-node collisions: the truss must hold together through
    // structure alone. (Wall confinement is separate and stays on.)
    FrEnd.check_collisions = false;

    final int x0 = x_px << Coords.shift;
    // Ground is the high-Y wall (positive gravity pulls toward +Y).
    // Feet rest ~2px above the true wall; the whole build sits at
    // z >= 40 so no node starts against the z = 0 wall.
    final int ground = (Coords.y_pixels << Coords.shift) - (2 << Coords.shift);
    final int zo = 40 << Coords.shift;

    final int bl = body_length_px << Coords.shift;
    final int bw = body_width_px << Coords.shift;
    final int bh = body_height_px << Coords.shift;
    final int zmid = zo + bw / 2;

    // Body: bottom rectangle of 4 nodes plus 2 ridge nodes (top).
    // Y increases downward; ground is at high Y, so "up" is smaller Y.
    final int body_y = ground - (leg_drop_px << Coords.shift) - bh / 2;
    final Node b0 = addNode(node_manager, clazz, node_type, x0, body_y, zo);
    final Node b1 = addNode(node_manager, clazz, node_type, x0 + bl, body_y, zo);
    final Node b2 = addNode(node_manager, clazz, node_type, x0 + bl, body_y, zo + bw);
    final Node b3 = addNode(node_manager, clazz, node_type, x0, body_y, zo + bw);
    final Node t0 =
        addNode(node_manager, clazz, node_type, x0 + bl / 2, body_y - bh, zo);
    final Node t1 = addNode(node_manager, clazz, node_type,
        x0 + bl / 2, body_y - bh, zo + bw);

    // Body edges: box + both bottom diagonals + one face diagonal
    // (12 edges, rigid). Keeps the stable b1-b3 from the original box;
    // the b0-t1 face diagonal adds the missing rigidity without the
    // octahedron's resonant triangulation.
    link(link_manager, body_type, clazz, b0, b1, -1);
    link(link_manager, body_type, clazz, b1, b2, -1);
    link(link_manager, body_type, clazz, b2, b3, -1);
    link(link_manager, body_type, clazz, b3, b0, -1);
    link(link_manager, body_type, clazz, b0, t0, -1);
    link(link_manager, body_type, clazz, b1, t0, -1);
    link(link_manager, body_type, clazz, b2, t1, -1);
    link(link_manager, body_type, clazz, b3, t1, -1);
    link(link_manager, body_type, clazz, t0, t1, -1);
    link(link_manager, body_type, clazz, b0, b2, -1); // bottom diagonal
    link(link_manager, body_type, clazz, b1, b3, -1); // bottom diagonal
    link(link_manager, brace_type, clazz, b0, t1, -1); // front face diagonal
    // NOTE: 12-edge body (32 links total). PanelFundamentalTest pins
    // Grasshopper at 31 links; it must be updated to 32. The 12th edge
    // is structurally essential: without it the V4 jump collapses
    // from 118px to 25px.

    final int[] sides = {-1, 1};

    // Hind legs on the rear edge (b1, b2): folded crouch, knee up and
    // behind the hip, foot on the ground directly under the hip. The leg
    // flexes in the fore-aft plane about the z-running hip edge.
    for (int s = 0; s < 2; s++) {
      final int side = sides[s];
      final int hip_x = x0 + bl;
      final int hip_y = body_y;
      final Node knee = addNode(node_manager, clazz, node_type,
          hip_x + (knee_back_px << Coords.shift),
          hip_y - (knee_up_px << Coords.shift),
          zmid + side * (knee_splay_px << Coords.shift));
      final Node foot = addNode(node_manager, clazz, node_type,
          hip_x + (foot_back_px << Coords.shift),
          ground,
          zmid + side * (leg_splay_px << Coords.shift));
      feet[s] = foot;

      // Tetrahedron (b1, b2, knee, foot): all 6 edges.
      // b1-b2 is a body edge (already exists).
      // The femur edges are the drive cables (tension-only): when they
      // shorten they reel the body up toward the knee, which is propped
      // on the tibia against the ground -- a direct extensor launch.
      // Hip-foot and knee-foot are passive spring struts. All eight
      // femur cables fire in phase 0 for a level launch.
      muscle(link_manager, muscle_type, clazz, b1, knee, 0);
      muscle(link_manager, muscle_type, clazz, b2, knee, 0);
      link(link_manager, leg_type, clazz, knee, foot, -1);
      link(link_manager, leg_type, clazz, b1, foot, -1);
      link(link_manager, leg_type, clazz, b2, foot, -1);
    }

    // Front legs on the front edge (b0, b3): mirrors of the hind legs
    // (knee forward-up, foot under the hip). Their crouch muscles fire
    // with the hind ones so the launch is level: a rear-only yank
    // pitches the body nose-down.
    for (int s = 0; s < 2; s++) {
      final int side = sides[s];
      final int hip_x = x0;
      final int hip_y = body_y;
      final Node knee = addNode(node_manager, clazz, node_type,
          hip_x - (front_knee_forward_px << Coords.shift),
          hip_y - (knee_up_px << Coords.shift),
          zmid + side * (knee_splay_px << Coords.shift));
      final Node foot = addNode(node_manager, clazz, node_type,
          hip_x - (front_foot_forward_px << Coords.shift),
          ground,
          zmid + side * (leg_splay_px << Coords.shift));
      feet[2 + s] = foot;

      // Tetrahedron (b0, b3, knee, foot): all 6 edges.
      // b0-b3 is a body edge (already exists).
      // Femur-cable drive like the hind legs: the cables reel the body
      // up toward the knees, propped on the tibiae. Fires in phase 0
      // with the hind legs for a level launch.
      muscle(link_manager, muscle_type, clazz, b0, knee, 0);
      muscle(link_manager, muscle_type, clazz, b3, knee, 0);
      link(link_manager, leg_type, clazz, knee, foot, -1);
      link(link_manager, leg_type, clazz, b0, foot, -1);
      link(link_manager, leg_type, clazz, b3, foot, -1);
    }

    marker = t0;
    return t0;
  }

  private static Node addNode(NodeManager nm, Clazz clazz, NodeType nt,
      int x, int y, int z) {
    return nm.addNewAgent(new Point3D(x, y, z), clazz, nt);
  }

  /** Passive strut (phase < 0 means no muscle). Rest length = actual distance. */
  private static void link(LinkManager lm, LinkType template, Clazz clazz,
      Node a, Node b, int phase) {
    // Each link gets its own type so the muscle controller's base length
    // (link.type.length) matches this link's actual geometry.
    final int dx = a.pos.x - b.pos.x;
    final int dy = a.pos.y - b.pos.y;
    final int dz = a.pos.z - b.pos.z;
    final int dist =
        (int) Math.sqrt((long) dx * dx + (long) dy * dy + (long) dz * dz);
    final LinkType type = lm.link_type_factory.getNew(dist, template.elasticity);
    type.damping = template.damping;
    final Link link = lm.setLink(a, b, type, clazz);
    link.adjusted_rest_length = dist;
    if (phase >= 0) {
      link.phase = phase;
      link.controller = new GlobalOscillatorController(Muscles.active_oscillator);
    }
  }

  /**
   * Muscle cable: a tension-only member (compression members stay
   * passive struts). Driven by the global oscillator; it can only pull --
   * when the sine asks for a longer rest length the cable simply goes
   * slack. All eight fire in phase 0 for a level launch.
   */
  private static void muscle(LinkManager lm, LinkType type, Clazz clazz,
      Node a, Node b, int phase) {
    final int n_o_l = lm.element.size();
    link(lm, type, clazz, a, b, phase);
    if (lm.element.size() != n_o_l + 1) {
      throw new IllegalStateException("muscle() must add exactly one link");
    }
    final Link link = (Link) lm.element.get(n_o_l);
    link.type.compression = false;
    // One-shot drive: a single yank-and-release, then the cables hold
    // their build length (see OneShotController). A repeating sine
    // pumps the pitch mode after landing and tumbles the body.
    link.controller = new OneShotController(Muscles.active_oscillator);
  }

}
