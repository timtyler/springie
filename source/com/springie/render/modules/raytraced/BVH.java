// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import java.util.Arrays;
import java.util.Comparator;

/**
 * A bounding volume hierarchy over the scene primitives. Built once per
 * frame on the calling thread, then shared read-only by the tile workers.
 */
final class BVH {
  private static final int MAX_LEAF_PRIMITIVES = 4;

  private static final int MAX_STACK = 64;

  private final Primitive[] primitives;

  private AABB[] bounds;

  private int[] left;

  private int[] right;

  private int[] start;

  private int[] count;

  private int nodes_used;

  BVH(Primitive[] primitives) {
    this.primitives = primitives.clone();
    final int capacity = Math.max(1, 2 * this.primitives.length);
    this.bounds = new AABB[capacity];
    this.left = new int[capacity];
    this.right = new int[capacity];
    this.start = new int[capacity];
    this.count = new int[capacity];
    this.nodes_used = 0;
    if (this.primitives.length > 0) {
      build(0, this.primitives.length);
    }
  }

  boolean isEmpty() {
    return this.primitives.length == 0;
  }

  private int build(int start, int end) {
    final int node = this.nodes_used++;
    final AABB node_bounds = new AABB();
    final AABB centroid_bounds = new AABB();
    final AABB scratch = new AABB();
    for (int i = start; i < end; i++) {
      scratch.min_x = Double.POSITIVE_INFINITY;
      scratch.min_y = Double.POSITIVE_INFINITY;
      scratch.min_z = Double.POSITIVE_INFINITY;
      scratch.max_x = Double.NEGATIVE_INFINITY;
      scratch.max_y = Double.NEGATIVE_INFINITY;
      scratch.max_z = Double.NEGATIVE_INFINITY;
      this.primitives[i].writeBounds(scratch);
      node_bounds.add(scratch);
      centroid_bounds.addPoint(scratch.centroidX(), scratch.centroidY(),
          scratch.centroidZ());
    }
    this.bounds[node] = node_bounds;

    if (end - start <= MAX_LEAF_PRIMITIVES) {
      this.left[node] = -1;
      this.start[node] = start;
      this.count[node] = end - start;
      return node;
    }

    final double dx = centroid_bounds.max_x - centroid_bounds.min_x;
    final double dy = centroid_bounds.max_y - centroid_bounds.min_y;
    final double dz = centroid_bounds.max_z - centroid_bounds.min_z;
    final int axis = dx >= dy && dx >= dz ? 0 : dy >= dz ? 1 : 2;

    final AABB sort_scratch_a = new AABB();
    final AABB sort_scratch_b = new AABB();
    Arrays.sort(this.primitives, start, end, new Comparator<Primitive>() {
      public int compare(Primitive p1, Primitive p2) {
        resetScratch(sort_scratch_a);
        resetScratch(sort_scratch_b);
        p1.writeBounds(sort_scratch_a);
        p2.writeBounds(sort_scratch_b);
        final double c1 = axis == 0 ? sort_scratch_a.centroidX()
            : axis == 1 ? sort_scratch_a.centroidY()
                : sort_scratch_a.centroidZ();
        final double c2 = axis == 0 ? sort_scratch_b.centroidX()
            : axis == 1 ? sort_scratch_b.centroidY()
                : sort_scratch_b.centroidZ();
        return c1 < c2 ? -1 : c1 > c2 ? 1 : 0;
      }
    });

    final int mid = (start + end) >>> 1;
    this.left[node] = build(start, mid);
    this.right[node] = build(mid, end);
    return node;
  }

  private static void resetScratch(AABB box) {
    box.min_x = Double.POSITIVE_INFINITY;
    box.min_y = Double.POSITIVE_INFINITY;
    box.min_z = Double.POSITIVE_INFINITY;
    box.max_x = Double.NEGATIVE_INFINITY;
    box.max_y = Double.NEGATIVE_INFINITY;
    box.max_z = Double.NEGATIVE_INFINITY;
  }

  /**
   * Finds the closest hit along the ray. The stack is caller-provided so
   * the traversal allocates nothing.
   */
  boolean intersect(Ray ray, Hit hit, int[] stack) {
    if (this.primitives.length == 0) {
      return false;
    }
    boolean found = false;
    int sp = 0;
    stack[sp++] = 0;
    while (sp > 0) {
      final int node = stack[--sp];
      if (!this.bounds[node].intersect(ray, hit.t)) {
        continue;
      }
      final int left_child = this.left[node];
      if (left_child < 0) {
        final int end = this.start[node] + this.count[node];
        for (int i = this.start[node]; i < end; i++) {
          if (this.primitives[i].intersect(ray, hit)) {
            found = true;
          }
        }
      } else {
        // Far child pushed first so the near child is visited first.
        final int right_child = this.right[node];
        final boolean hit_left = this.bounds[left_child].intersect(ray,
            hit.t);
        final boolean hit_right = this.bounds[right_child].intersect(ray,
            hit.t);
        if (hit_left && hit_right) {
          // Order by projected centre distance along the ray.
          final AABB bl = this.bounds[left_child];
          final AABB br = this.bounds[right_child];
          final double cl = (bl.centroidX() - ray.ox) * ray.dx
              + (bl.centroidY() - ray.oy) * ray.dy
              + (bl.centroidZ() - ray.oz) * ray.dz;
          final double cr = (br.centroidX() - ray.ox) * ray.dx
              + (br.centroidY() - ray.oy) * ray.dy
              + (br.centroidZ() - ray.oz) * ray.dz;
          if (cl < cr) {
            stack[sp++] = right_child;
            stack[sp++] = left_child;
          } else {
            stack[sp++] = left_child;
            stack[sp++] = right_child;
          }
        } else if (hit_left) {
          stack[sp++] = left_child;
        } else if (hit_right) {
          stack[sp++] = right_child;
        }
      }
      if (sp > MAX_STACK - 2) {
        // Degenerate depth; should not happen for sane scenes, but never
        // overflow the stack.
        break;
      }
    }
    return found;
  }
}
