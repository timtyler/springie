// This program has been placed into the public domain by its author.

package com.springie.modification.faces;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;

import com.springie.elements.links.Link;
import com.springie.elements.nodes.Node;

/**
 * Finds polygonal faces in a wireframe made of nodes and links.
 *
 * <p>The algorithm treats the selected nodes and links as a graph and
 * looks for its minimal cycles:
 *
 * <ol>
 * <li>For every link (u, v), run a breadth-first search from u to v
 * that is forbidden from using that link.</li>
 * <li>Every shortest u-v path the search finds, closed by the link
 * itself, is a shortest cycle through that link - i.e. one face.</li>
 * <li>Cycles are found once per edge they contain, so duplicates are
 * merged by node set.</li>
 * </ol>
 *
 * <p>On a cube wireframe this yields exactly the six square faces; on a
 * triangulated patch it yields the triangles. A diagonal across a
 * would-be quad splits it into the two triangles, because the quad is
 * no longer a shortest cycle through any of its edges. A dangling
 * chain of links contains no cycle and produces no faces, and links
 * touching unselected nodes are ignored.
 *
 * <p>The number of distinct shortest paths enumerated per link is
 * capped, so a pathological selection cannot hang the UI.
 */
public class FaceCycleFinder {
  /** Safety cap on the distinct shortest paths enumerated per link. */
  static final int MAX_PATHS_PER_LINK = 32;

  private static final class Edge {
    final int to;
    final int link;

    Edge(int to, int link) {
      this.to = to;
      this.link = link;
    }
  }

  private FaceCycleFinder() {
    // static only
  }

  /**
   * @return one node list per face found, each in cycle order
   */
  public static ArrayList<ArrayList<Node>> findFaceCycles(
      ArrayList<Node> nodes, ArrayList<Link> links) {
    final int n = nodes.size();
    final ArrayList<ArrayList<Node>> faces = new ArrayList<>();
    if (n < 3 || links.size() < 3) {
      return faces;
    }

    final IdentityHashMap<Node, Integer> index = new IdentityHashMap<>(n * 2);
    for (int i = 0; i < n; i++) {
      index.put(nodes.get(i), Integer.valueOf(i));
    }

    final ArrayList<ArrayList<Edge>> adj = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      adj.add(new ArrayList<Edge>());
    }

    final int m = links.size();
    final int[] link_a = new int[m];
    final int[] link_b = new int[m];
    Arrays.fill(link_a, -1);
    for (int li = 0; li < m; li++) {
      final Link link = links.get(li);
      final Integer a = index.get(link.nodes[0]);
      final Integer b = index.get(link.nodes[1]);
      if (a == null || b == null || a.intValue() == b.intValue()) {
        continue; // touches an unselected node, or is a self-loop
      }
      link_a[li] = a.intValue();
      link_b[li] = b.intValue();
      adj.get(a.intValue()).add(new Edge(b.intValue(), li));
      adj.get(b.intValue()).add(new Edge(a.intValue(), li));
    }

    final HashSet<String> seen = new HashSet<>();
    final int[] dist = new int[n];
    final int[] queue = new int[n];

    for (int li = 0; li < m; li++) {
      final int u = link_a[li];
      final int v = link_b[li];
      if (u < 0) {
        continue;
      }

      // BFS from u, forbidden from using link li.
      Arrays.fill(dist, -1);
      dist[u] = 0;
      queue[0] = u;
      int head = 0;
      int tail = 1;
      while (head < tail) {
        final int x = queue[head++];
        for (Edge e : adj.get(x)) {
          if (e.link == li || dist[e.to] >= 0) {
            continue;
          }
          dist[e.to] = dist[x] + 1;
          queue[tail++] = e.to;
        }
      }
      if (dist[v] <= 1) {
        continue; // unreachable, or a parallel link (a 2-node "cycle")
      }

      // Predecessors along the shortest paths.
      final ArrayList<ArrayList<Integer>> prev = new ArrayList<>(n);
      for (int i = 0; i < n; i++) {
        prev.add(new ArrayList<Integer>());
      }
      for (int x = 0; x < n; x++) {
        if (dist[x] < 0) {
          continue;
        }
        for (Edge e : adj.get(x)) {
          if (e.link != li && dist[e.to] == dist[x] + 1) {
            prev.get(e.to).add(Integer.valueOf(x));
          }
        }
      }

      final ArrayList<int[]> paths = new ArrayList<>();
      collectPaths(prev, v, u, dist[v], new int[dist[v] + 1], paths);

      for (int[] p : paths) {
        // p runs v -> ... -> u; the face closes u -> v via the link.
        final int[] cycle = new int[p.length];
        for (int i = 0; i < p.length; i++) {
          cycle[i] = p[p.length - 1 - i];
        }
        if (!seen.add(canonicalKey(cycle))) {
          continue;
        }
        final ArrayList<Node> face_nodes = new ArrayList<>(cycle.length);
        for (int c : cycle) {
          face_nodes.add(nodes.get(c));
        }
        faces.add(face_nodes);
      }
    }

    return faces;
  }

  private static void collectPaths(ArrayList<ArrayList<Integer>> prev,
      int node, int target, int depth, int[] path, ArrayList<int[]> out) {
    if (out.size() >= MAX_PATHS_PER_LINK) {
      return;
    }
    path[depth] = node;
    if (node == target) {
      out.add(path.clone());
      return;
    }
    for (Integer p : prev.get(node)) {
      collectPaths(prev, p.intValue(), target, depth - 1, path, out);
      if (out.size() >= MAX_PATHS_PER_LINK) {
        return;
      }
    }
  }

  private static String canonicalKey(int[] cycle) {
    final int[] sorted = cycle.clone();
    Arrays.sort(sorted);
    return Arrays.toString(sorted);
  }
}
