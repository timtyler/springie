// This code has been placed into the public domain by its author

package com.springie.elements.links;

import java.util.ArrayList;
import java.util.List;

import com.springie.FrEnd;
import com.springie.context.ContextMananger;
import com.springie.elements.base.BaseElementManager;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.lists.ListOfIntegers;
import com.springie.elements.nodes.Node;
import com.springie.geometry.Point3D;
import com.springie.render.CachedLink;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;
import com.springie.utilities.math.SquareRoot;
import com.tifsoft.deprecated.OldMethods;

public class LinkManager extends BaseElementManager<Link> {
  static int agent_counter;

  static Node temp_node;

  public LinkTypeFactory link_type_factory = new LinkTypeFactory();

  public LinkManager() {
    reset();
  }

  /**
   * Sets a link between e1 and e2, with target length lenth, elasticity, colour
   * and status flags specified
   */
  public final Link setLink(Node node1, Node node2, LinkType type, Clazz clazz) {
    final int last = this.element.size();
    node1.list_of_links.add(last);
    node2.list_of_links.add(last);

    final Link link = new Link(node1, node2, type, clazz);
    this.element.add(link);

    return link;
  }

  // dangerous... TODO: eliminate this code...
  public final Link setLink(LinkType type, Clazz clazz) {
    final Link l = new Link(null, null, type, clazz);
    this.element.add(l);

    return l;
  }

  /**
   * Sets a link between e1 and e2, which is otherwise a clone of the specified
   * link, lk
   */
  public final Link setLink(Node e1, Node e2, Link lk) {
    final Link l = new Link(e1, e2, lk);

    final int last = this.element.size();
    e1.list_of_links.add(last);
    e2.list_of_links.add(last);

    this.element.add(l);

    // add this link to the list of links associated with the node...
    // e1.addLink(l);
    // e2.addLink(l);

    return l;
  }

  public final boolean isNodeLinkedToNode(Node e1, Node e2) {
    final int n_o_l = this.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link l = (Link) this.element.get(temp);
      if ((l.nodes[0] == e1) && (l.nodes[1] == e2)) {
        return true;
      }

      if ((l.nodes[0] == e2) && (l.nodes[1] == e1)) {
        return true;
      }
    }

    return false;
  }

  // need to re-write this so that it uses local list of links stored with
  // node...
  // doesn't quite work properly...

  /**
   * deletes all links between e1 and e2
   */
  public final void deleteAllLinksBetween(Node e1, Node e2) {
    final int n_o_l = this.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link l = (Link) this.element.get(temp);

      if ((l.nodes[0] == e1) && (l.nodes[1] == e2)) {
        killNumberedLink(temp);
      }

      if ((l.nodes[0] == e2) && (l.nodes[1] == e1)) {
        killNumberedLink(temp);
      }
    }
  }

  /**
   * returns true iff there's a *visible* link between e1 and e2
   */
  public final boolean isThereALinkBetween(Node node1, Node node2) {
    final ListOfIntegers list_of_links = node1.list_of_links;
    final int n_o_l = list_of_links.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final int i = list_of_links.retreive(temp);
      final Link link = (Link) this.element.get(i);
      if (!link.type.hidden) {
        if (link.nodes[0] == node2) {
          return true;
        }
        if (link.nodes[1] == node2) {
          return true;
        }
      }
    }
    return false;
  }

  // need to rewrite this so that it uses local list of links stored with
  // node...
  /**
   * returns true iff there's a *visible* link between e1 and e2
   */
  public final boolean ZoldIsThereALinkBetween(Node e1, Node e2) {
    final int n_o_l = this.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link l = (Link) this.element.get(temp);
      if (!l.type.hidden) {
        if ((l.nodes[0] == e1) && (l.nodes[1] == e2)) {
          return true;
        }

        if ((l.nodes[0] == e2) && (l.nodes[1] == e1)) {
          return true;
        }
      }
    }

    return false;
  }

  // need to rewrite this so that it uses local list of links stored with
  // node...
  /**
   * returns the link between e1 and e2, or null if no such link exists
   */
  public final Link getLinkBetween(Node e1, Node e2) {
    final int n_o_l = this.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link l = (Link) this.element.get(temp);
      if ((l.nodes[0] == e1) && (l.nodes[1] == e2)) {
        return l;
      }

      if ((l.nodes[0] == e2) && (l.nodes[1] == e1)) {
        return l;
      }
    }

    return null;
  }

  // need to rewrite this so that it uses local list of links stored with
  // node...
  public final boolean ZisThereALinkFromTo(Node e1, Node e2) {
    final int n_o_l = this.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link l = (Link) this.element.get(temp);
      // one way only...
      if ((l.nodes[0] == e1) && (l.nodes[1] == e2)) {
        return true;
      }
    }

    return false;
  }

  // need to rewrite this so that it uses local list of links stored with
  // node...
  final Link ZgetLinkFromTo(Node e1, Node e2) {
    final int n_o_l = this.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link l = (Link) this.element.get(temp);
      if ((l.nodes[0] == e1) && (l.nodes[1] == e2)) {
        return l;
      }

    }

    return null;
  }

  // hmm...
  /**
   * Destroys all links to Node e Should use list of links...?
   */
  public final void killAllLinks(Node e) {
    final int n_o_l = this.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link l = (Link) this.element.get(temp);
      if ((l.nodes[0] == e) || (l.nodes[1] == e)) {
        killNumberedLink(temp);
      }
    }
  }

  // hmm...
  /**
   * Destroys the last link that was created on Node e
   */

  public final boolean killLastLink(Node e) {
    final int n_o_l = this.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link l = (Link) this.element.get(temp);
      if ((l.nodes[0] == e) || (l.nodes[1] == e)) {
        // if () {//TODO!!!!
        killNumberedLink(temp);
        return true;
      }
    }

    return false;
  }

  public final void killNumberedLink(int n) {
    // final Link link = (Link) this.element.get(n);
    // link.nodes[0].list_of_links.remove(n);
    // link.nodes[1].list_of_links.remove(n);
    this.element.remove(n);
  }

  final int getNumberOfLink(Link lk) {
    final int n_o_l = this.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link l = (Link) this.element.get(temp);
      if (l == lk) {
        return temp;
      }
    }

    return -1;
  }

  public final void killSpecifiedLink(Link lk) {
    final int temp = getNumberOfLink(lk);
    killNumberedLink(temp);
  }

  // TODO
  // public final void drawTheLinks() {
  // final int n_o_l = this.element.size();
  // for (int temp = n_o_l; --temp >= 0;) {
  // final Link l = (Link) this.element.get(temp);
  // l.draw();
  // }
  // }

  // public final void performLinkUpdate() {
  // if (!FrEnd.paused) {
  // if (!FrEnd.links_disabled) {
  // exerciseTheLinks();
  // ContextMananger.getNodeManager().electrostatic.repel();
  // ContextMananger.getNodeManager().applyViscousDrag();
  // }
  // }
  // }

// ; // *should* do this in some sort of random order?
//  public final void applyElasticForceAll() {
//    final int n_o_l = this.element.size();
//    for (int temp = n_o_l; --temp >= 0;) {
//      final Link l = (Link) this.element.get(temp);
//      if (!l.type.disabled) {
//        l.applyElasticForce();
//      }
//    }
//  }
//
//  public final void applyDampingForceAll() {
//    final int n_o_l = this.element.size();
//    for (int temp = n_o_l; --temp >= 0;) {
//      final Link l = (Link) this.element.get(temp);
//      if (!l.type.disabled) {
//        l.applyDampingForce();
//      }
//    }
//  }
//
//  public final void applyElasticForceOrigAll() {
//    final int n_o_l = this.element.size();
//    for (int temp = n_o_l; --temp >= 0;) {
//      final Link l = (Link) this.element.get(temp);
//      if (!l.type.disabled) {
//        l.applyElasticForceOrig2();
//      }
//    }
//  }
    
  public final void applyElasticForceOrigAll() {
    final int n_o_l = this.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link l = (Link) this.element.get(temp);
      if (!l.type.disabled) {
        l.applyElasticForceOrig();
      }
    }
  }
    
  
  public final Link isThereOne(int x, int y) {
    int min = Integer.MAX_VALUE;
    Link best = null;
    final int n_o_l = this.element.size();
    final int n_o_l_2 = ContextMananger.getNodeManager().renderer.renderer_link.array.length;

    if (n_o_l != n_o_l_2) {
      return null;
    }

    for (int temp = n_o_l; --temp >= 0;) {
      final Link l = (Link) this.element.get(temp);
      final CachedLink cl = ContextMananger.getNodeManager().renderer.renderer_link.array[temp];
      // return name;t;// this.element.get(temp);
      if (!l.type.hidden || FrEnd.render_hidden_links) {
        for (int i = 0; i < cl.preserved_node_start.length - 1; i++) {
          final Point3D point0 = cl.preserved_node_start[i];
          final Point3D point1 = cl.preserved_node_end[i];
          final java.awt.Polygon awt_polygon = cl.getStrutPolygon(point0,
              point1, l.getThicknesss());

          if (OldMethods.isInsidePolygon(x >> Coords.shift, y >> Coords.shift,
              awt_polygon)) {
            if (l.nodes[0].pos.z < min) {
              best = l;
              min = l.nodes[0].pos.z;
            }
          }
        }
      }
    }

    return best;
  }

  // co_ords, line start, line end...
  static final int distanceSquaredToLine(int _x, int _y, int _x1, int _y1,
      int _x2, int _y2) {
    // unit vector at right angles to line.
    final int dx0 = _y1 - _y2;
    final int dy0 = _x2 - _x1;

    int dx1 = dx0 >> Coords.shift;
    int dy1 = dy0 >> Coords.shift;

    int dist = SquareRoot.fastSqrt(1 + (dx1 * dx1) + (dy1 * dy1)); // pixels...
    // dist = dist << Coords.shift;

    dx1 = dx0 / dist;
    dy1 = dy0 / dist;

    final int dx2 = (_x - _x1) >> Coords.shift; // in pixels...
    final int dy2 = (_y - _y1) >> Coords.shift; // in pixels...

    // Dot product: (vector from one end to point).(normal)...
    dist = (dx1 * dx2) + (dy1 * dy2);

    return (dist < 0) ? -dist : dist;
  }

  // co_ords, line start, line end...
  static final int distanceToLine(int _x, int _y, int _x1, int _y1, int _x2,
      int _y2) {
    final int dist = SquareRoot.fastSqrt(1 + distanceSquaredToLine(_x, _y, _x1,
        _y1, _x2, _y2)) << 5;

    return dist;
  }

  public final void deselectAll() {
    final int n_o_l = this.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link l = (Link) this.element.get(temp);
      if (l.type.selected) {
        l.type.selected = false;
        // BinGrid.RepaintAll = true;
      }
    }
    FrEnd.updateGUIToReflectSelectionChange();
  }

  public final void deselectAll(int colour) {
    final int n_o_l = this.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link l = (Link) this.element.get(temp);
      if (l.clazz.colour == colour) {
        if (l.type.selected) {
          l.type.selected = false;
        }
      }
    }
    FrEnd.updateGUIToReflectSelectionChange();
  }

  public final void selectionInvert() {
    final int n_o_l = this.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link link = (Link) this.element.get(temp);
      if (!link.type.hidden || FrEnd.render_hidden_links) {
        link.setSelectedFiltered(!link.type.selected);
      }
    }
    FrEnd.updateGUIToReflectSelectionChange();
  }

  public final void selectAll() {
    final int n_o_l = this.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link link = (Link) this.element.get(temp);
      if (!link.type.hidden || FrEnd.render_hidden_links) {
        if (!link.type.selected) {
          link.setSelectedFiltered(true);
        }
      }
    }
    FrEnd.updateGUIToReflectSelectionChange();
  }

  public final void selectAll(int colour) {
    final int n_o_l = this.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link link = (Link) this.element.get(temp);
      if (link.clazz.colour == colour) {
        if (!link.type.hidden || FrEnd.render_hidden_links) {
          link.setSelectedFiltered(true);
        }
      }
    }
    FrEnd.updateGUIToReflectSelectionChange();
  }

  public final void deleteSelected() {
    final int n_o_l = this.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link l = (Link) this.element.get(temp);
      if (l.type.selected) {
        killSpecifiedLink(l);
      }
    }
    FrEnd.updateGUIToReflectSelectionChange();
  }

  public final void setColourOfSelected(int c) {
    final int n_o_l = this.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link l = (Link) this.element.get(temp);
      if (l.type.selected) {
        l.clazz.colour = c;
        RendererDelegator.repaint_all_objects = true;
      }
    }
  }

  public final void setSizeOfSelected(int s) {
    final int n_o_l = this.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link l = (Link) this.element.get(temp);
      if (l.type.selected) {
        l.type.setLength(s);
      }
    }
  }

  public final void setElasticityOfSelected(int e) {
    final int n_o_l = this.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link l = (Link) this.element.get(temp);
      if (l.type.selected) {
        l.type.setElasticity(e);
      }
    }
  }

  public final void setStiffnessOfSelected(int damping) {
    final int n_o_l = this.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link l = (Link) this.element.get(temp);
      if (l.type.selected) {
        l.type.damping = damping;
      }
    }
  }

  public final void setLengthOfSelected(int length) {
    final int n_o_l = this.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link l = (Link) this.element.get(temp);
      if (l.type.selected) {
        l.type.setLength(length);
      }
    }
  }

  public final void setRadiusOfSelected(int radius) {
    final int n_o_l = this.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link l = (Link) this.element.get(temp);
      if (l.type.selected) {
        l.type.radius = radius;
      }
    }
  }

  public final Link getFirstSelectedLink() {
    final int n_o_l = this.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link l = (Link) this.element.get(temp);
      if (l.type.selected) {
        return l;
      }
    }

    return null;
  }

  public boolean isSelection() {
    final int n_o_l = this.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link l = (Link) this.element.get(temp);
      if (l.type.selected) {
        return true;
      }
    }

    return false;
  }

  public int getNumberOfSelected() {
    int number = 0;
    final int total = this.element.size();
    for (int temp = 0; temp < total; temp++) {
      final Link link = this.element.get(temp);
      if (link.type.selected) {
        number++;
      }
    }

    return number;
  }

  public List<Node> getListOfNodesOnSelectedLinks() {
    final List<Node> list_of_nodes = new ArrayList<>();
    final int total = this.element.size();
    for (int temp = 0; temp < total; temp++) {
      final Link link = this.element.get(temp);
      if (link.isSelected()) {
        final int non = link.nodes.length;
        for (int i = 0; i < non; i++) {
          final Node node = link.nodes[i];
          list_of_nodes.add(node);
        }
      }
    }
    return list_of_nodes;
  }
}
