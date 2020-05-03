package com.springie.modification.links;

import com.springie.FrEnd;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.links.LinkType;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.geometry.Point3D;
import com.springie.geometry.Tuple3D;
import com.springie.modification.post.PostModification;
import com.springie.modification.pre.PrepareToModifyLinkTypes;
import com.springie.modification.pre.PrepareToModifyNodeTypes;
import com.tifsoft.Forget;

public class ExtendLinks {
  NodeManager node_manager;

  LinkManager link_manager;

  PostModification post_modification;

  public ExtendLinks(NodeManager node_manager) {
    this.node_manager = node_manager;
    this.link_manager = node_manager.getLinkManager();
    this.post_modification = new PostModification(node_manager);

    prepare();
  }

  public void extend(float sf) {
    //Log.log("Extend:" + sf);
    Forget.about(sf);
    final int n_o_l = this.link_manager.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link l = (Link) this.link_manager.element.get(temp);
      final int x = l.nodes[0].pos.x;
      final float scale = (float) (0.5 + 0.3 * Math.sin(x / 12000.0));
      if (l.type.selected) {
        extend(l, scale);
      }
    }

    FrEnd.postCleanup();
  }

  private void extend(Link l, float sf) {
    Forget.about(sf);
    if (l.nodes.length == 2) {
      final Node n1 = l.nodes[0];
      final Node n2 = l.nodes[1];

      final int x1 = n2.pos.x;
      final float scale1 = (float) (0.5 + 0.3 * Math.sin(x1 / 12000.0));

      final Tuple3D vector = new Tuple3D(n2.pos);      
      vector.subtractTuple3D(n1.pos);      
      vector.multiplyBy(scale1);
      
      final Node n3 = this.node_manager.addNewAgent(n2);
      final Point3D n3_new_pos = new Point3D(n2.pos);
      n3_new_pos.addTuple3D(vector);
      n3.pos = n3_new_pos;
      
      //Clazz new_c1 = ClazzFactory.getSimilar(l.clazz);
      
      makeLink(n2, n3, l.type, l.clazz);

      final int x2 = n1.pos.x;
      final float scale2 = (float) (0.5 + 0.3 * Math.sin(x2 / 12000.0));

      final Tuple3D vector2 = new Tuple3D(n2.pos);      
      vector2.subtractTuple3D(n1.pos);      
      vector2.multiplyBy(scale2);

      final Node n4 = this.node_manager.addNewAgent(n1);
      final Point3D n4_new_pos = new Point3D(n1.pos);
      n4_new_pos.subtractTuple3D(vector);
      n4.pos = n4_new_pos;      

      makeLink(n1, n4, l.type, l.clazz);
    }
  }

  private void makeLink(final Node n1, final Node n2, LinkType type, Clazz clazz) {
    this.link_manager.setLink(n1, n2, type, clazz);
  }

  void prepare() {
    final PrepareToModifyLinkTypes prepare_l = new PrepareToModifyLinkTypes(
        this.node_manager.getLinkManager());
    prepare_l.prepare();

    final PrepareToModifyNodeTypes prepare_n = new PrepareToModifyNodeTypes(
        this.node_manager);
    prepare_n.prepare();
  }
}
