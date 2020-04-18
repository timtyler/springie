// * Read in information from files...

package com.springie.io.in.readers.dxf;


class ReaderDXFPoint {
  /**
   * code x - Point.
   */
  public double x;

  /**
   * code y - Point.
   */
  public double y;

  /**
   * code z - Point.
   */
  public double z;

  /**
   * Constructor
   */
  ReaderDXFPoint() {
    this.x = 0.0;
    this.y = 0.0;
    this.z = 0.0;
  }

  /**
   * Constructor
   * 
   * @param x
   *          The x coordinate.
   * @param y
   *          The y coordinate.
   * @param z
   *          The z coordinate.
   */
  ReaderDXFPoint(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  /**
   * * Set the values of this 3D point based on polar coordinate values TODO.
   * 
   * @param dist
   *          The distance.
   * @param azim
   *          The azimuth.
   * @param elev
   *          The elevation.
   * @return Reference to this 3D point.
   */
  // private
  // ReaderDXFPoint setP(double dist, double azim, double elev)
  // {
  // double radius = dist * Math.cos(elev);
  // x = radius * Math.cos(azim);
  // y = radius * Math.sin(azim);
  // z = dist * Math.sin(elev);
  //
  // return this;
  // }
  /**
   * Set the coordinates based on the coordinate values of a 3D point object.
   * 
   * @param PntW
   *          The source 3D point.
   * @return Reference to this 3D point.
   */
  ReaderDXFPoint set(ReaderDXFPoint PntW) {
    this.x = PntW.x;
    this.y = PntW.y;
    this.z = PntW.z;

    return this;
  }

  // /**
  // * Calculate cross product.
  // * From <I>Fast Algorithms for 3D-Graphics</I>.<br>
  // * <CODE>
  // * #define Cross_product(n, a, b)\
  // * ((n)[X] = (a)[Y] * (b)[Z] - (a)[Z] * (b)[Y],\
  // * (n)[Y] = (a)[Z] * (b)[X] - (a)[X] * (b)[Z],\
  // * (n)[Z] = (a)[X] * (b)[Y] - (a)[Y] * (b)[X])
  // * </CODE>
  // * @param a A 3D point.
  // * @param b A 3D point.
  // */
  private void crossProduct(ReaderDXFPoint a, ReaderDXFPoint b) {
    // System.out.println("PointW:a=" + a + ",dist=" + a.distance());
    // System.out.println("PointW:b=" + b + ",dist=" + b.distance());
    this.x = a.y * b.z - a.z * b.y;
    this.y = a.z * b.x - a.x * b.z;
    this.z = a.x * b.y - a.y * b.x;
    // System.out.println("PointW:c=" + this + ",dist=" + this.distance());
  }

  /**
   * Calculate Ax_out and Ay_out axes from Az_in using the <I>Arbitrary Axis
   * Algorithrm</I>. It is assumed that Az_in has already been normalized<br>
   * 1.0 / 64.0 == .015625 exactly
   * 
   * @param Ax_out
   *          An axis.
   * @param Ay_out
   *          An axis.
   * @param Az_in
   *          TODO
   */
  static void calcAAA(ReaderDXFPoint Ax_out, ReaderDXFPoint Ay_out,
      ReaderDXFPoint Az_in) {
    final ReaderDXFPoint Wy = new ReaderDXFPoint(0, 1, 0);

    final ReaderDXFPoint Wz = new ReaderDXFPoint(0, 0, 1);

    if (Math.abs(Az_in.x) < 1.0 / 64.0 && Math.abs(Az_in.y) < 1.0 / 64.0) {
      // System.out.println("calcAAA: Wy x Az_in");
      Ax_out.crossProduct(Wy, Az_in);
    } else {
      // System.out.println("calcAAA: Wz x Az_in");
      Ax_out.crossProduct(Wz, Az_in);
    }
    Ax_out.normalize();
    // System.out.println("calcAAA: Az_in x Ax_out");
    Ay_out.crossProduct(Az_in, Ax_out);
    Ay_out.normalize();
    // System.out.println("calcAAA: Ax_out =" + Ax_out + ",dist=" +
    // Ax_out.distance());
    // System.out.println("calcAAA: Ay_out =" + Ay_out + ",dist=" +
    // Ay_out.distance());
    // System.out.println("calcAAA: Az_in =" + Az_in + ",dist=" + Az_in
    // .distance());
  }

  /**
   * Make unit vector.
   */
  public void normalize() {
    final double vecLength = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);

    this.x = this.x / vecLength;
    this.y = this.y / vecLength;
    this.z = this.z / vecLength;
  }

  /**
   * Clone point.
   * 
   * @exception InternalError
   *              If CloneNotSupportedException.
   * @return Reference to a new point.
   */
  protected Object clone() {
    try {
     final ReaderDXFPoint zclone = (ReaderDXFPoint) super.clone();
      return zclone;
    } catch (CloneNotSupportedException e) {
      throw new InternalError(e.toString());
    }
  }

  /**
   * Stringify this 3D point.
   * 
   * @return this 3D point.
   */
  public String toString() {
    return "ReaderDXFPoint[" + this.x + " " + this.y + " " + this.z + "]";
  }
}
