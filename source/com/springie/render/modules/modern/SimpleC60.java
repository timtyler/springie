// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * C60 - the buckminsterfullerene, i.e. the truncated icosahedron: the classic
 * football pattern of 12 pentagonal faces and 20 hexagonal faces
 * (60 vertices, 90 edges).
 *
 * Built by truncating a regular icosahedron one third of the way along
 * every edge: each original vertex becomes a pentagon, each original
 * triangular face becomes a hexagon.
 */
public class SimpleC60 extends ObjectBase {
  public SimpleC60() {
    final Double3D[] ico_points = {
        new Double3D(-0.68345, 0, 1.10585),
        new Double3D(0.68345, 0, 1.10585),
        new Double3D(-0.68345, 0, -1.10585),
        new Double3D(0.68345, 0, -1.10585),
        new Double3D(0, 1.10585, 0.68345),
        new Double3D(0, 1.10585, -0.68345),
        new Double3D(0, -1.10585, 0.68345),
        new Double3D(0, -1.10585, -0.68345),
        new Double3D(1.10585, 0.68345, 0),
        new Double3D(-1.10585, 0.68345, 0),
        new Double3D(1.10585, -0.68345, 0),
        new Double3D(-1.10585, -0.68345, 0), };

    final int[][] ico_faces = {{0, 4, 1 }, {0, 9, 4 }, {4, 9, 5 },
        {4, 5, 8 }, {1, 4, 8 }, {1, 8, 10 }, {3, 10, 8 }, {3, 8, 5 },
        {2, 3, 5 }, {2, 7, 3 }, {3, 7, 10 }, {6, 10, 7 }, {6, 7, 11 },
        {0, 6, 11 }, {0, 1, 6 }, {1, 10, 6 }, {0, 11, 9 }, {2, 9, 11 },
        {2, 5, 9 }, {2, 11, 7 }, };

    // Truncation points: map edge (lo < hi) -> {index near lo, index near hi}.
    final List<Double3D> points = new ArrayList<>();
    final Map<Long, int[]> edge_cuts = new HashMap<>();
    for (final int[] face : ico_faces) {
      for (int e = 0; e < 3; e++) {
        final int a = face[e];
        final int b = face[(e + 1) % 3];
        final int lo = Math.min(a, b);
        final int hi = Math.max(a, b);
        final Long key = ((long) lo << 32) | hi;
        if (!edge_cuts.containsKey(key)) {
          final Double3D pa = ico_points[lo];
          final Double3D pb = ico_points[hi];
          final int i_lo = points.size();
          points.add(lerp(pa, pb, 1.0 / 3.0));
          final int i_hi = points.size();
          points.add(lerp(pa, pb, 2.0 / 3.0));
          edge_cuts.put(key, new int[] {i_lo, i_hi});
        }
      }
    }

    // Neighbours of each original vertex, for the pentagons.
    final List<List<Integer>> neighbours = new ArrayList<>();
    for (int i = 0; i < ico_points.length; i++) {
      neighbours.add(new ArrayList<Integer>());
    }
    for (final Long key : edge_cuts.keySet()) {
      final int lo = (int) (key >> 32);
      final int hi = (int) (key & 0xffffffffL);
      neighbours.get(lo).add(hi);
      neighbours.get(hi).add(lo);
    }

    final List<int[]> faces = new ArrayList<>();

    // Each truncated vertex becomes a pentagon; order its neighbours
    // by polar angle around the vertex so the face is a proper ring.
    for (int v = 0; v < ico_points.length; v++) {
      final List<Integer> ring = orderByAngle(ico_points[v],
          neighbours.get(v), ico_points);
      final int[] pentagon = new int[ring.size()];
      for (int i = 0; i < ring.size(); i++) {
        pentagon[i] = truncNear(edge_cuts, v, ring.get(i));
      }
      faces.add(fixWinding(points, pentagon));
    }

    // Each truncated triangle becomes a hexagon.
    for (final int[] face : ico_faces) {
      final int a = face[0];
      final int b = face[1];
      final int c = face[2];
      final int[] hexagon = {truncNear(edge_cuts, a, b),
          truncNear(edge_cuts, a, c), truncNear(edge_cuts, c, a),
          truncNear(edge_cuts, c, b), truncNear(edge_cuts, b, c),
          truncNear(edge_cuts, b, a), };
      faces.add(fixWinding(points, hexagon));
    }

    // Normalise to the same radius as the icosahedron it was cut from.
    for (final Double3D p : points) {
      p.normalize();
      p.x *= 1.3;
      p.y *= 1.3;
      p.z *= 1.3;
    }

    this.points = points.toArray(new Double3D[0]);
    this.faces = faces.toArray(new int[0][]);
  }

  private static Double3D lerp(Double3D a, Double3D b, double t) {
    return new Double3D(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t,
        a.z + (b.z - a.z) * t);
  }

  private static int truncNear(Map<Long, int[]> edge_cuts, int from, int to) {
    final int lo = Math.min(from, to);
    final int hi = Math.max(from, to);
    final int[] pair = edge_cuts.get(((long) lo << 32) | hi);
    return from == lo ? pair[0] : pair[1];
  }

  private static double dot(Double3D a, Double3D b) {
    return a.x * b.x + a.y * b.y + a.z * b.z;
  }

  // Orders neighbours of a vertex by polar angle around it, so they form
  // a ring suitable for a polygon face.
  private static List<Integer> orderByAngle(Double3D vertex,
      List<Integer> neighbours, Double3D[] ico_points) {
    final Double3D axis = new Double3D(vertex);
    axis.normalize();
    final Double3D ref = Math.abs(axis.z) < 0.9 ? new Double3D(0, 0, 1)
        : new Double3D(0, 1, 0);
    final Double3D u = new Double3D(
        ref.x - axis.x * dot(ref, axis),
        ref.y - axis.y * dot(ref, axis),
        ref.z - axis.z * dot(ref, axis));
    u.normalize();
    final Double3D u2 = axis.crossProduct(u);

    final List<Integer> ordered = new ArrayList<>(neighbours);
    ordered.sort((p, q) -> Double.compare(angle(ico_points, vertex, p, u, u2),
        angle(ico_points, vertex, q, u, u2)));
    return ordered;
  }

  private static double angle(Double3D[] ico_points, Double3D vertex,
      int neighbour, Double3D u, Double3D u2) {
    final Double3D d = ico_points[neighbour].subtract(vertex);
    return Math.atan2(dot(d, u2), dot(d, u));
  }

  // Ensures the face vertices wind the outward-facing way (the polygon
  // renderer backface-culls on projected winding).
  private static int[] fixWinding(List<Double3D> points, int[] face) {
    // Newell's method for the face normal.
    double nx = 0;
    double ny = 0;
    double nz = 0;
    double cx = 0;
    double cy = 0;
    double cz = 0;
    final int n = face.length;
    for (int i = 0; i < n; i++) {
      final Double3D p = points.get(face[i]);
      final Double3D q = points.get(face[(i + 1) % n]);
      nx += (p.y - q.y) * (p.z + q.z);
      ny += (p.z - q.z) * (p.x + q.x);
      nz += (p.x - q.x) * (p.y + q.y);
      cx += p.x;
      cy += p.y;
      cz += p.z;
    }
    if (nx * cx + ny * cy + nz * cz < 0) {
      final int[] reversed = new int[n];
      for (int i = 0; i < n; i++) {
        reversed[i] = face[n - 1 - i];
      }
      return reversed;
    }
    return face;
  }
}
