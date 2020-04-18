// This code has been placed into the public domain by its author

package com.springie.elements.links;

import com.springie.FrEnd;
import com.springie.elements.base.BaseElement;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.nodes.Node;
import com.springie.explosions.fragments.LineFragmentManager;
import com.springie.geometry.Point3D;
import com.springie.gui.panels.controls.PanelControlsSelectLinks;
import com.springie.render.Coords;
import com.springie.utilities.math.SquareRoot;
import com.springie.utilities.random.JUR;
import com.springie.world.World;

public class Link extends BaseElement {
  public Node[] nodes = new Node[0];

  public int[] lengths;

  public LinkType type;

  public static int number_of_strut_render_divisions = 2;

  public static int number_of_cable_render_divisions = 1;

  public static final int force_scale = 300;

  public static final int elasticity_global = 2000;

  // 800
  public static final int damping_global = 400;

  public static World temp_private_world;

  static final int MAX_DIAMETER = 16;

  public static final int INVISIBLE = 0;

  public static final int LONG = 0;

  public static final int SHORT = 1;

  public static final int number_of_dots = 2;

  public static final int log_number_of_dots = 1;

  public static int link_display_struts_type = LinkRenderType.MULTIPLE;

  public static int link_display_cables_type = LinkRenderType.MULTIPLE;

  
  public static int link_display_length = SHORT;

  public Link(Node e1, Node e2, LinkType type, Clazz clazz) {
    this.nodes = new Node[2];
    this.nodes[0] = e1;
    this.nodes[1] = e2;
    this.type = type;
    this.clazz = clazz;
  }

  Link(Node e1, Node e2, Link l) {
    set(l);
    this.nodes[0] = e1;
    this.nodes[1] = e2;
  }

  public void addNode(Node temp_node, int idx) {
    final int size = this.nodes.length;
    if (idx >= size) {
      // if (size > 2) throw new RuntimeException("PROGRAMING ERROR");
      Node[] temp = new Node[size + 1];
      System.arraycopy(this.nodes, 0, temp, 0, size);
      this.nodes = temp;
    }

    this.nodes[idx] = temp_node;
  }

  void set(Node e1, Node e2, int l, int e) {
    this.nodes = new Node[2];
    this.nodes[0] = e1;
    this.nodes[1] = e2;
    this.type = new LinkType(l, e);
  }

  void set(Node e1, Node e2, LinkType type, Clazz clazz) {
    this.nodes = new Node[2];
    this.nodes[0] = e1;
    this.nodes[1] = e2;
    this.type = type;
    this.clazz = clazz;
  }

  // DANGER
  void set(Link l) {
    this.type = l.type;
    this.clazz = l.clazz;

    this.nodes = new Node[2];
    this.nodes[0] = l.nodes[0];
    this.nodes[1] = l.nodes[1];
  }

  void set(Node e1, Node e2, Link l) {
    set(l);
    // override the defaults...
    this.nodes = new Node[2];
    this.nodes[0] = e1;
    this.nodes[1] = e2;
  }

  public int getActualLength() {
    final int total = this.nodes.length;
    int actual_length_squared = 0;
    for (int section = 0; section < total - 1; section++) {
      final int delta_x = (this.nodes[section].pos.x - this.nodes[section + 1].pos.x) >> Coords.shift;
      final int delta_y = (this.nodes[section].pos.y - this.nodes[section + 1].pos.y) >> Coords.shift;
      final int delta_z = (this.nodes[section].pos.z - this.nodes[section + 1].pos.z) >> Coords.shift;

      actual_length_squared += (delta_x * delta_x) + (delta_y * delta_y)
          + (delta_z * delta_z);
    }
    return (SquareRoot.fastSqrt(1 + actual_length_squared)) << Coords.shift;
  }

  
  void applyElasticForceOrig() {
    if (this.type.elasticity == 0) {
      return;
    }

    // first get preferred length...
    int total_rest_length = this.type.length >> Coords.shift;

    if (total_rest_length < 1) {
      total_rest_length = 1;
    }

    final int number_of_nodes = this.nodes.length;

    final int total_actual_length = calculateTotalActualLength(number_of_nodes);

    if (total_rest_length > total_actual_length) {
      if (!this.type.compression) {
        return;
      }
    } else if (!this.type.tension) {
      return;
    }

    final int temp_damping = (damping_global * this.type.damping) >> 8;
    
    final int temp_elasticity = (elasticity_global * this.type.elasticity) >> 8;

    final int scaled_force = ((total_actual_length * force_scale) / total_rest_length)
        - force_scale;

    for (int i = 0; i < number_of_nodes - 1; i++) {
      final Node n0 = this.nodes[i];
      final Node n1 = this.nodes[i + 1];
      final int d_x = (n0.pos.x - n1.pos.x) >> Coords.shift;
      final int d_y = (n0.pos.y - n1.pos.y) >> Coords.shift;
      final int d_z = (n0.pos.z - n1.pos.z) >> Coords.shift;
      final int actual_length_squared = (d_x * d_x) + (d_y * d_y) + (d_z * d_z);
      final int actual_length = SquareRoot.fastSqrt(1 + actual_length_squared);

      final int d_dx = n0.velocity.x - n1.velocity.x;
      final int d_dy = n0.velocity.y - n1.velocity.y;
      final int d_dz = n0.velocity.z - n1.velocity.z;

      final int unit_vector_x = (d_x << Coords.shift) / actual_length;
      final int unit_vector_y = (d_y << Coords.shift) / actual_length;
      final int unit_vector_z = (d_z << Coords.shift) / actual_length;

      final int parting = ((unit_vector_x * d_dx) + (unit_vector_y * d_dy) + (unit_vector_z * d_dz)) >> Coords.shift;

      final int damping_value = (parting * temp_damping + 128) >> 8;
      final int force_value = scaled_force * temp_elasticity;
      final int val_a = ((force_value + damping_value) + 2048) >> 12;

      int force_x_a = unit_vector_x * val_a;
      int force_y_a = unit_vector_y * val_a;
      int force_z_a = unit_vector_z * val_a;

      n0.velocity_delta.x -= force_x_a;
      n0.velocity_delta.y -= force_y_a;
      n0.velocity_delta.z -= force_z_a;

      n1.velocity_delta.x += force_x_a;
      n1.velocity_delta.y += force_y_a;
      n1.velocity_delta.z += force_z_a;
    }
  }
  
//  void applyElasticForceOrig2() {
//    if (this.type.elasticity == 0) {
//      return;
//    }
//
//    // first get preferred length...
//    int total_rest_length = this.type.length >> Coords.shift;
//
//    if (total_rest_length < 1) {
//      total_rest_length = 1;
//    }
//
//    final int number_of_nodes = this.nodes.length;
//
//    final int total_actual_length = calculateTotalActualLength(number_of_nodes);
//
//    if (total_rest_length > total_actual_length) {
//      if (!this.type.compression) {
//        return;
//      }
//    } else if (!this.type.tension) {
//      return;
//    }
//
//    final int temp_damping = (damping_global * this.type.damping) >> 8;
//    
//    final int temp_elasticity = (elasticity_global * this.type.elasticity) >> 8;
//    
//    final int scaled_force = ((total_actual_length * force_scale) / total_rest_length)
//        - force_scale;
//
//    for (int i = 0; i < number_of_nodes - 1; i++) {
//      final Node n0 = this.nodes[i];
//      final Node n1 = this.nodes[i + 1];
//      final int d_x = (n0.pos.x - n1.pos.x) >> Coords.shift;
//      final int d_y = (n0.pos.y - n1.pos.y) >> Coords.shift;
//      final int d_z = (n0.pos.z - n1.pos.z) >> Coords.shift;
//      final int actual_length_squared = (d_x * d_x) + (d_y * d_y) + (d_z * d_z);
//      final int actual_length = SquareRoot.fastSqrt(1 + actual_length_squared);
//
//      //final int d_dx = n0.velocity.x - n1.velocity.x;
//      //final int d_dy = n0.velocity.y - n1.velocity.y;
//      //final int d_dz = n0.velocity.z - n1.velocity.z;
//
//      final int unit_vector_x = (d_x << Coords.shift) / actual_length;
//      final int unit_vector_y = (d_y << Coords.shift) / actual_length;
//      final int unit_vector_z = (d_z << Coords.shift) / actual_length;
//
//      final int force_value = scaled_force * temp_elasticity;
//      final int val_a = (force_value + 2048) >> 12;
//
//      int force_x_a = unit_vector_x * val_a;
//      int force_y_a = unit_vector_y * val_a;
//      int force_z_a = unit_vector_z * val_a;
//
//      n0.velocity_delta.x -= force_x_a;
//      n0.velocity_delta.y -= force_y_a;
//      n0.velocity_delta.z -= force_z_a;
//
//      n1.velocity_delta.x += force_x_a;
//      n1.velocity_delta.y += force_y_a;
//      n1.velocity_delta.z += force_z_a;
//
//      final int d_dx = n0.velocity.x - n0.velocity_delta.x - n1.velocity.x + n1.velocity_delta.x;
//      final int d_dy = n0.velocity.y - n0.velocity_delta.y - n1.velocity.y + n1.velocity_delta.y;
//      final int d_dz = n0.velocity.z - n0.velocity_delta.z - n1.velocity.z + n1.velocity_delta.z;
//
//      int parting = (d_x * d_dx) + (d_y * d_dy) + (d_z * d_dz);
//      parting = parting / actual_length;
//      final int damping_value = (parting * temp_damping + 128) >> 8;
//
//      final int val_a2 = (damping_value + 2048) >> 12;
//
//      int force_x_a2 = unit_vector_x * val_a2;
//      int force_y_a2 = unit_vector_y * val_a2;
//      int force_z_a2 = unit_vector_z * val_a2;
//
//      n0.velocity_delta.x -= force_x_a2;
//      n0.velocity_delta.y -= force_y_a2;
//      n0.velocity_delta.z -= force_z_a2;
//
//      n1.velocity_delta.x += force_x_a2;
//      n1.velocity_delta.y += force_y_a2;
//      n1.velocity_delta.z += force_z_a2;
//    }
//  }
//  
//  void applyElasticForce() {
//    final int number_of_nodes = this.nodes.length;
//
//    if (this.lengths == null) {
//      this.lengths = new int[number_of_nodes];
//    }
//
//    if (this.type.elasticity == 0) {
//      return;
//    }
//
//    // first get preferred length...
//    int total_rest_length = this.type.length >> Coords.shift;
//
//    if (total_rest_length < 1) {
//      total_rest_length = 1;
//    }
//
//    final int total_actual_length = calculateTotalActualLength(number_of_nodes);
//
//    if (total_rest_length > total_actual_length) {
//      if (!this.type.compression) {
//        return;
//      }
//    } else if (!this.type.tension) {
//      return;
//    }
//
//    final int temp_elasticity = (elasticity_global * this.type.elasticity) >> 8;
//
//    final int scaled_force = ((total_actual_length * force_scale) / total_rest_length)
//        - force_scale;
//
//    for (int i = 0; i < number_of_nodes - 1; i++) {
//      final Node n0 = this.nodes[i];
//      final Node n1 = this.nodes[i + 1];
//      final int d_x = (n0.pos.x - n1.pos.x) >> Coords.shift;
//      final int d_y = (n0.pos.y - n1.pos.y) >> Coords.shift;
//      final int d_z = (n0.pos.z - n1.pos.z) >> Coords.shift;
//      int actual_length = getActualLength(d_x, d_y, d_z);
//
//      // cache it for later...
//      this.lengths[i] = actual_length;
//
//      final int unit_vector_x = (d_x << Coords.shift) / actual_length;
//      final int unit_vector_y = (d_y << Coords.shift) / actual_length;
//      final int unit_vector_z = (d_z << Coords.shift) / actual_length;
//
//      final int force_value = scaled_force * temp_elasticity;
//      final int val_a = (force_value + 2048) >> 12;
//
//      int force_x_a = unit_vector_x * val_a;
//      int force_y_a = unit_vector_y * val_a;
//      int force_z_a = unit_vector_z * val_a;
//
//      n0.velocity_delta.x -= force_x_a;
//      n0.velocity_delta.y -= force_y_a;
//      n0.velocity_delta.z -= force_z_a;
//
//      n1.velocity_delta.x += force_x_a;
//      n1.velocity_delta.y += force_y_a;
//      n1.velocity_delta.z += force_z_a;
//    }
//  }
//
//  void applyDampingForce() {
//    final int number_of_nodes = this.nodes.length;
//
//    final int temp_damping = (damping_global * this.type.damping) >> 8;
//
//    for (int i = 0; i < number_of_nodes - 1; i++) {
//      final Node n0 = this.nodes[i];
//      final Node n1 = this.nodes[i + 1];
//      
//      final int d_x = (n0.pos.x - n1.pos.x) >> Coords.shift;
//      final int d_y = (n0.pos.y - n1.pos.y) >> Coords.shift;
//      final int d_z = (n0.pos.z - n1.pos.z) >> Coords.shift;
//
//      int actual_length = this.lengths[i];
//      if (actual_length == 0) {
//        actual_length = getActualLength(d_x, d_y, d_z);
//      }
//
//      final int d_dx = n0.velocity.x - n0.velocity_delta.x - n1.velocity.x + n1.velocity_delta.x;
//      final int d_dy = n0.velocity.y - n0.velocity_delta.y - n1.velocity.y + n1.velocity_delta.y;
//      final int d_dz = n0.velocity.z - n0.velocity_delta.z - n1.velocity.z + n1.velocity_delta.z;
//
//      final int unit_vector_x = (d_x << Coords.shift) / actual_length;
//      final int unit_vector_y = (d_y << Coords.shift) / actual_length;
//      final int unit_vector_z = (d_z << Coords.shift) / actual_length;
//
//      int parting = (unit_vector_x * d_dx) + (unit_vector_y * d_dy) + (unit_vector_z * d_dz);
//      parting = parting >> Coords.shift;
//
//      final int damping_value = (parting * temp_damping + 128) >> 8;
//      final int val_a = (damping_value + 2048) >> 12;
//
//      int force_x_a = unit_vector_x * val_a;
//      int force_y_a = unit_vector_y * val_a;
//      int force_z_a = unit_vector_z * val_a;
//
//      n0.velocity_delta.x -= force_x_a;
//      n0.velocity_delta.y -= force_y_a;
//      n0.velocity_delta.z -= force_z_a;
//
//      n1.velocity_delta.x += force_x_a;
//      n1.velocity_delta.y += force_y_a;
//      n1.velocity_delta.z += force_z_a;
//    }
//  }
//
//  private int getActualLength(final int d_x, final int d_y, final int d_z) {
//    final int actual_length_squared = (d_x * d_x) + (d_y * d_y) + (d_z * d_z);
//    int actual_length = SquareRoot.fastSqrt(1 + actual_length_squared);
//    if (actual_length == 0) {
//      actual_length++;
//    }
//    return actual_length;
//  }

  private int calculateTotalActualLength(final int number_of_nodes) {
    int total_length = 0;
    for (int i = 0; i < number_of_nodes - 1; i++) {
      final Node n0 = this.nodes[i];
      final Node n1 = this.nodes[i + 1];
      final int d_x = (n0.pos.x - n1.pos.x) >> Coords.shift;
      final int d_y = (n0.pos.y - n1.pos.y) >> Coords.shift;
      final int d_z = (n0.pos.z - n1.pos.z) >> Coords.shift;
      final int actual_length_squared = (d_x * d_x) + (d_y * d_y) + (d_z * d_z);
      final int actual_length = SquareRoot.fastSqrt(1 + actual_length_squared);

      total_length += actual_length;
    }
    return total_length;
  }

  void explodeThisLink() {
    final JUR rnd = new JUR();

    int start_x = 0;
    int start_y = 0;
    int d_x = 0;
    int d_y = 0;

    int temp2 = SquareRoot.fastSqrt(1 + (d_x * d_x) + (d_y * d_y));

    if (temp2 > 20) {
      temp2 = temp2 / 10;
    } else {
      temp2 = 4;
    }

    d_x = (d_x << Coords.shift) / temp2;
    d_y = (d_y << Coords.shift) / temp2;

    int start_dx = this.nodes[0].velocity.x;
    int start_dy = this.nodes[0].velocity.y;

    int d_dx = (this.nodes[1].velocity.x - this.nodes[0].velocity.x) / temp2;
    int d_dy = (this.nodes[1].velocity.y - this.nodes[0].velocity.y) / temp2;

    if (FrEnd.explosions) {
      if (!FrEnd.xor) {
        for (int temp = 0; temp < temp2; temp++) {
          int temp3 = rnd.nextInt();
          final int temp4 = (temp3 << 8) >> 23;
          temp3 = temp3 >> 23;

          LineFragmentManager.add(start_x, start_y, start_x + d_x, start_y
              + d_y, start_dx + temp3, start_dy + temp4, 4);
          start_x += d_x;
          start_y += d_y;
          start_dx += d_dx;
          start_dy += d_dy;
        }
      }
    }
  }

  // in pixels...
  public int getThicknesss() {
    return this.type.radius >> Coords.shift;
  }

  public int getDisplayType() {
    final int type = !this.type.compression ? Link.link_display_cables_type
        : Link.link_display_struts_type;
    return type;
  }

  /**
   * Given an this.node (which is assumed to be at one end of this link) return
   * the this.node at the other end
   */
  public final Node theOtherEnd(Node e) {
    if (this.nodes[0] == e) {
      return this.nodes[this.nodes.length - 1];
    }

    return this.nodes[0];
  }

  public boolean hasNode(Node candidate) {
    final int total = this.nodes.length;
    for (int section = 0; section < total; section++) {
      if (this.nodes[section] == candidate) {
        return true;
      }
    }

    return false;
  }

  public boolean isHidden() {
    return this.type.hidden;
  }

  public boolean isSelected() {
    return this.type.selected;
  }

  public void setSelected(boolean selected) {
    this.type.selected = selected;    
  }

  public void setSelectedFiltered(boolean selected) {
    if (selected && !PanelControlsSelectLinks.comparator.qualifies(this)) {
      return;
    }
    this.type.selected = selected;    
  }

  public Point3D getCoordinatesOfCentrePoint() {
    final Point3D centre = new Point3D(0, 0, 0);
    final int total = this.nodes.length;
    for (int section = 0; section < total; section++) {
      final Node n = this.nodes[section];
      centre.addTuple3D(n.pos);
    }

    centre.divideBy(total);

    return centre;
  }
}