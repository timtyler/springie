package com.springie.elements.nodes;

import com.springie.elements.base.BaseType;
import com.springie.utilities.random.Hortensius32Fast;

public class NodeType extends BaseType {
  public boolean pinned;
  public int charge;
  public int counter;

  static Hortensius32Fast static_rnd = new Hortensius32Fast();

  protected NodeType() {
    // Mass is functional now: default to the reference mass, which responds
    // to forces exactly as every node did when mass was ignored.
    this.log_mass = REFERENCE_LOG_MASS;
    setSize(18);
  }

  protected NodeType(NodeType current) {
    makeEqualTo(current);
  }

  public void setMass(int log_mass) {
    this.log_mass = log_mass;
  }

  /**
   * Log2 mass that responds to forces exactly as every node did before mass
   * became functional: the accumulated force delta is applied unscaled.
   * Mass is stored logarithmically, so multiplying by inverse mass is just a
   * shift -- log_mass 17 responds half as much, 15 responds twice as much.
   */
  public static final int REFERENCE_LOG_MASS = 16;

  /**
   * Scales an accumulated force delta by inverse node mass. Pure integer,
   * no division: heavier nodes shift right, lighter nodes shift left.
   * Right shifts truncate toward zero (never floor), so mirrored negative
   * deltas scale symmetrically; left shifts saturate instead of overflowing.
   */
  public static int applyInverseMass(final int force_delta, final int log_mass) {
    final int shift = log_mass - REFERENCE_LOG_MASS;
    if (shift == 0) {
      return force_delta;
    }
    if (shift > 0) {
      // Heavier node: divide by 2^shift, truncating toward zero.
      if (shift >= 31) {
        return 0;
      }
      final long v = force_delta;
      return (int) (v >= 0 ? v >> shift : -((-v) >> shift));
    }
    // Lighter node: multiply by 2^-shift, saturating on overflow.
    final int mul = -shift;
    if (mul >= 31) {
      return force_delta >= 0 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
    }
    final long scaled = ((long) force_delta) << mul;
    if (scaled > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    if (scaled < Integer.MIN_VALUE) {
      return Integer.MIN_VALUE;
    }
    return (int) scaled;
  }

  public void setSize(int r) {
    this.radius = r;
  }

  public void makeEqualTo(NodeType t) {
    this.log_mass = t.log_mass;
    this.charge = t.charge;
    this.hidden = t.hidden;
    this.radius = t.radius;
    this.pinned = t.pinned;
    this.selected = t.selected;
    this.counter = t.counter;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + charge;
    result = prime * result + counter;
    result = prime * result + (pinned ? 1231 : 1237);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    NodeType other = (NodeType) obj;
    if (charge != other.charge) {
      return false;
    }
    if (counter != other.counter) {
      return false;
    }
    if (pinned != other.pinned) {
      return false;
    }
    return true;
  }
}
