package com.springie.io.in.readers.dxf;

public class ReaderDXFMatrix {
  /**
   * Transformation matrix element.
   */
  private double m00 = 1;

  /**
   * Transformation matrix element.
   */
  private double m01;

  /**
   * Transformation matrix element.
   */
  private double m02;

  /**
   * Transformation matrix element.
   */
  private double m03;

  /**
   * Transformation matrix element.
   */
  private double m10;

  /**
   * Transformation matrix element.
   */
  private double m11 = 1;

  /**
   * Transformation matrix element.
   */
  private double m12;

  /**
   * Transformation matrix element.
   */
  private double m13;

  /**
   * Transformation matrix element.
   */
  private double m20;

  /**
   * Transformation matrix element.
   */
  private double m21;

  /**
   * Transformation matrix element.
   */
  private double m22 = 1;

  /**
   * Transformation matrix element.
   */
  private double m23;

  /**
   * Transformation matrix element.
   */
  private double m30;

  /**
   * Transformation matrix element.
   */
  private double m31;

  /**
   * Transformation matrix element.
   */
  private double m32;

  /**
   * Transformation matrix element.
   */
  private double m33 = 1;

  // Set the matrix to rotate a point to match the A axes
  void mtxRotateAxes_World_to_Local(ReaderDXFPoint Ax, ReaderDXFPoint Ay,
      ReaderDXFPoint Az) {
    final double mr00 = Ax.x;
    final double mr01 = Ay.x;
    final double mr02 = Az.x;
    final double mr03 = 0;
    final double mr10 = Ax.y;
    final double mr11 = Ay.y;
    final double mr12 = Az.y;
    final double mr13 = 0;
    final double mr20 = Ax.z;
    final double mr21 = Ay.z;
    final double mr22 = Az.z;
    final double mr23 = 0;
    final double mr30 = 0;
    final double mr31 = 0;
    final double mr32 = 0;
    final double mr33 = 1;

    // mtxPreMultiply(m_r);
    double ma00 = mr00 * this.m00 + mr01 * this.m10 + mr02 * this.m20 + mr03
        * this.m30;
    double ma01 = mr00 * this.m01 + mr01 * this.m11 + mr02 * this.m21 + mr03
        * this.m31;
    double ma02 = mr00 * this.m02 + mr01 * this.m12 + mr02 * this.m22 + mr03
        * this.m32;
    double ma03 = mr00 * this.m03 + mr01 * this.m13 + mr02 * this.m23 + mr03
        * this.m33;
    double ma10 = mr10 * this.m00 + mr11 * this.m10 + mr12 * this.m20 + mr13
        * this.m30;
    double ma11 = mr10 * this.m01 + mr11 * this.m11 + mr12 * this.m21 + mr13
        * this.m31;
    double ma12 = mr10 * this.m02 + mr11 * this.m12 + mr12 * this.m22 + mr13
        * this.m32;
    double ma13 = mr10 * this.m03 + mr11 * this.m13 + mr12 * this.m23 + mr13
        * this.m33;
    double ma20 = mr20 * this.m00 + mr21 * this.m10 + mr22 * this.m20 + mr23
        * this.m30;
    double ma21 = mr20 * this.m01 + mr21 * this.m11 + mr22 * this.m21 + mr23
        * this.m31;
    double ma22 = mr20 * this.m02 + mr21 * this.m12 + mr22 * this.m22 + mr23
        * this.m32;
    double ma23 = mr20 * this.m03 + mr21 * this.m13 + mr22 * this.m23 + mr23
        * this.m33;
    double ma30 = mr30 * this.m00 + mr31 * this.m10 + mr32 * this.m20 + mr33
        * this.m30;
    double ma31 = mr30 * this.m01 + mr31 * this.m11 + mr32 * this.m21 + mr33
        * this.m31;
    double ma32 = mr30 * this.m02 + mr31 * this.m12 + mr32 * this.m22 + mr33
        * this.m32;
    double ma33 = mr30 * this.m03 + mr31 * this.m13 + mr32 * this.m23 + mr33
        * this.m33;

    this.m00 = ma00;
    this.m01 = ma01;
    this.m02 = ma02;
    this.m03 = ma03;
    this.m10 = ma10;
    this.m11 = ma11;
    this.m12 = ma12;
    this.m13 = ma13;
    this.m20 = ma20;
    this.m21 = ma21;
    this.m22 = ma22;
    this.m23 = ma23;
    this.m30 = ma30;
    this.m31 = ma31;
    this.m32 = ma32;
    this.m33 = ma33;
  }

  /**
   * Transform TODO
   * 
   * @param p1
   *          The 3D point TODO
   * @param p2
   *          The 3D point TODO
   * @return The transformed point.
   */
  ReaderDXFPoint mtxTransformPoint(ReaderDXFPoint p1, ReaderDXFPoint p2) {
    p2.x = this.m00 * p1.x + this.m01 * p1.y + this.m02 * p1.z + this.m03;
    p2.y = this.m10 * p1.x + this.m11 * p1.y + this.m12 * p1.z + this.m13;
    p2.z = this.m20 * p1.x + this.m21 * p1.y + this.m22 * p1.z + this.m23;

    return p2;
  }

  /**
   * Stringify this matrix.
   * 
   * @return The matrix.
   */
  public String toString() {
    return "DXFxMatrix 0[" + this.m00 + " " + this.m01 + " " + this.m02 + " "
        + this.m03 + "]\n" + "              1[" + this.m10 + " " + this.m11
        + " " + this.m12 + " " + this.m13 + "]\n" + "              2["
        + this.m20 + " " + this.m21 + " " + this.m22 + " " + this.m23 + "]\n"
        + "              3[" + this.m30 + " " + this.m31 + " " + this.m32 + " "
        + this.m33 + "]";
  }
}
