package com.springie.io.out.writers.fdl;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Vector;

import uk.org.fdl.object.FDLObjectBraceList;
import uk.org.fdl.object.FDLObjectBracketList;
import uk.org.fdl.object.FDLObjectChain;
import uk.org.fdl.object.FDLObjectIdentifier;
import uk.org.fdl.object.FDLObjectNumber;
import uk.org.fdl.writer.FDLWriter;

import com.springie.FrEnd;
import com.springie.context.ContextMananger;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.electrostatics.ElectrostaticRepulsion;
import com.springie.elements.faces.Face;
import com.springie.elements.faces.FaceManager;
import com.springie.elements.faces.FaceType;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.links.LinkType;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.elements.nodes.NodeType;
import com.springie.io.out.DuplicateLinkRemover;
import com.springie.io.out.GarbageCollection;
import com.springie.io.out.WriteFloatingPoint;
import com.springie.modification.post.PostModification;
import com.springie.modification.redundancy.RedundancyRemover;
import com.springie.render.Coords;
import com.springie.utilities.log.Log;
import com.springie.world.World;

public class WriterFDL {
  // scale factor - causes problems if not equal to 1.
  //
  // Plan to make this work...
  // ...and then to make it more widely available -
  // e.g.: in the GUI...
  static final int scale_factor = 1;

  Writer out;

  Vector nodes;

  NodeManager node_manager;

  LinkManager link_manager;

  public WriterFDL(NodeManager node_manager) {
    this.node_manager = node_manager;
    this.link_manager = node_manager.getLinkManager();
  }

  public void write(String filename) {
    final String s = generateString();

    try {
      try {
        this.out = new FileWriter(filename);
        this.out.write("[fdl version=1.0]\n");
        this.out.write(s);
      } finally {
        this.out.flush();
        this.out.close();
      }
    } catch (IOException e) {
      Log.log("Error in write: " + e);
    }
  }

  public String generateString() {
    new DuplicateLinkRemover(this.node_manager).removeDuplicateLinks();
    new GarbageCollection(this.node_manager).cleanUp();
    new RedundancyRemover(this.node_manager).removeRedundancy();
    new PostModification(this.node_manager).thoroughCleanup();

    this.nodes = new Vector();

    final FDLObjectChain chain_uni = new FDLObjectChain(":");
    chain_uni.add(new FDLObjectIdentifier("universe"));

    final FDLObjectBracketList list_attr = new FDLObjectBracketList();

    final String g_v = "" + World.gravity_strength;
    final FDLObjectChain chain_gravity_strength = new FDLObjectChain("=");
    chain_gravity_strength.add(new FDLObjectIdentifier("gravity_strength"));
    chain_gravity_strength.add(new FDLObjectNumber(g_v));
    list_attr.add(chain_gravity_strength);

    if (World.gravity_active) {
      final String g_b = "" + World.gravity_active;
      final FDLObjectChain chain_gravity_active = new FDLObjectChain("=");
      chain_gravity_active.add(new FDLObjectIdentifier("gravity_active"));
      chain_gravity_active.add(new FDLObjectNumber(g_b));
      list_attr.add(chain_gravity_active);
    }

    // 2D
    if (!FrEnd.three_d) {
      final FDLObjectChain chain_dimensions = new FDLObjectChain("=");
      chain_dimensions.add(new FDLObjectIdentifier("dimensions"));
      chain_dimensions.add(new FDLObjectNumber("2"));
      list_attr.add(chain_dimensions);
    }

    final ElectrostaticRepulsion electrostatic = ContextMananger.getNodeManager().electrostatic;
    if (electrostatic.charge_active) {
      final String c_b = "" + electrostatic.charge_active;
      final FDLObjectChain chain_charge_active = new FDLObjectChain("=");
      chain_charge_active.add(new FDLObjectIdentifier("charge_active"));
      chain_charge_active.add(new FDLObjectNumber(c_b));
      list_attr.add(chain_charge_active);
    }

    chain_uni.add(list_attr);

    final FDLObjectBraceList uni = new FDLObjectBraceList();
    recursivelyOutputAllNodes(uni);
    recursivelyOutputAllLinks(uni);
    recursivelyOutputAllFaces(uni);

    chain_uni.add(uni);

    final FDLObjectChain chain = new FDLObjectChain(":");
    chain.add(new FDLObjectIdentifier("tensegrity"));

    final FDLObjectBracketList attributes = new FDLObjectBracketList();
    
    final FDLObjectChain chain_version = new FDLObjectChain("=");
    chain_version.add(new FDLObjectIdentifier("version"));
    chain_version.add(new FDLObjectNumber("1.0"));
    attributes.add(chain_version);
    
    chain.add(attributes);
     
    final FDLObjectBraceList ten = new FDLObjectBraceList();
    
    ten.add(chain_uni);
    
    chain.add(ten);

    return FDLWriter.toString(chain); //.makeString();
  }
  
  private void recursivelyOutputAllLinks(FDLObjectBraceList uni) {
    final FDLObjectChain chain = new FDLObjectChain("=");
    chain.add(new FDLObjectIdentifier("links"));

    final FDLObjectBraceList links = new FDLObjectBraceList();
    final boolean any = outputLinkClasses(links);

    chain.add(links);
    if (any) {
      uni.add(chain);
    }
  }

  private void recursivelyOutputAllFaces(FDLObjectBraceList uni) {
    final FDLObjectChain chain = new FDLObjectChain("=");
    chain.add(new FDLObjectIdentifier("polygons"));

    final FDLObjectBraceList polygons = new FDLObjectBraceList();
    final boolean any = outputFaceClasses(polygons);

    chain.add(polygons);
    if (any) {
      uni.add(chain);
    }
  }

  private void recursivelyOutputAllNodes(FDLObjectBraceList uni) {
    final FDLObjectChain chain = new FDLObjectChain("=");
    chain.add(new FDLObjectIdentifier("nodes"));

    final FDLObjectBraceList nodes = new FDLObjectBraceList();
    final boolean any = outputNodeClasses(nodes);

    chain.add(nodes);
    if (any) {
      uni.add(chain);
    }
  }

  private boolean outputNodeClasses(FDLObjectBraceList uni) {
    final int clazz_number = this.node_manager.clazz_factory.array.size();
    boolean some = false;
    for (int c = 0; c < clazz_number; c++) {
      final Clazz clazz = (Clazz) this.node_manager.clazz_factory.array
          .elementAt(c);
      final FDLObjectChain chain = new FDLObjectChain(":");
      chain.add(new FDLObjectIdentifier("class"));
      final FDLObjectBracketList list_attr = new FDLObjectBracketList();

      outputColour(clazz, list_attr);

      chain.add(list_attr);

      final FDLObjectBraceList list_nodes = new FDLObjectBraceList();

      final boolean any = outputNodeTypes(list_nodes, clazz);

      chain.add(list_nodes);

      if (any) {
        uni.add(chain);
        some = true;
      }
    }

    return some;
  }

  private void outputColour(final Clazz clazz, FDLObjectBraceList tag_type) {
    //final WriterWRL writer = new WriterWRL(this.node_manager);

    final float t = (clazz.colour >>> 24) / 255f;
    if (t < 0.999f) {
      final FDLObjectChain chain = new FDLObjectChain("=");
      chain.add(new FDLObjectIdentifier("opacity"));
      chain.add(new FDLObjectNumber(WriteFloatingPoint.emit(t)));
      tag_type.add(chain);
    }

    final float r = ((clazz.colour >>> 16) & 0xFF) / 255f;
    if (r < 0.999f) {
      final FDLObjectChain chain = new FDLObjectChain("=");
      chain.add(new FDLObjectIdentifier("red"));
      chain.add(new FDLObjectNumber(WriteFloatingPoint.emit(r)));
      tag_type.add(chain);
    }

    final float g = ((clazz.colour >>> 8) & 0xFF) / 255f;
    if (g < 0.999f) {
      final FDLObjectChain chain = new FDLObjectChain("=");
      chain.add(new FDLObjectIdentifier("green"));
      chain.add(new FDLObjectNumber(WriteFloatingPoint.emit(g)));
      tag_type.add(chain);
    }

    final float b = (clazz.colour & 0xFF) / 255f;
    if (b < 0.999f) {
      final FDLObjectChain chain = new FDLObjectChain("=");
      chain.add(new FDLObjectIdentifier("blue"));
      chain.add(new FDLObjectNumber(WriteFloatingPoint.emit(b)));
      tag_type.add(chain);
    }
  }

  private boolean outputNodeTypes(final FDLObjectBraceList uni, Clazz clazz) {
    final int node_type_number = ContextMananger.getNodeManager().node_type_factory.array
        .size();

    boolean some = false;
    for (int nt = 0; nt < node_type_number; nt++) {
      final FDLObjectChain chain = new FDLObjectChain(":");
      chain.add(new FDLObjectIdentifier("type"));

      final FDLObjectBracketList list_attr = new FDLObjectBracketList();

      final NodeType node_type = (NodeType) ContextMananger.getNodeManager().node_type_factory.array
          .elementAt(nt);
      final FDLObjectChain chain_radius = new FDLObjectChain("=");
      chain_radius.add(new FDLObjectIdentifier("radius"));
      chain_radius.add(new FDLObjectNumber(""
          + (node_type.radius / scale_factor)));
      list_attr.add(chain_radius);

      final FDLObjectChain chain_charge = new FDLObjectChain("=");
      chain_charge.add(new FDLObjectIdentifier("charge"));
      chain_charge.add(new FDLObjectNumber("" + node_type.charge));
      list_attr.add(chain_charge);

      if (node_type.hidden) {
        final FDLObjectChain chain_hidden = new FDLObjectChain("=");
        chain_hidden.add(new FDLObjectIdentifier("hidden"));
        chain_hidden.add(new FDLObjectIdentifier("true"));
        list_attr.add(chain_hidden);
      }

      if (node_type.pinned) {
        final FDLObjectChain chain_fixed = new FDLObjectChain("=");
        chain_fixed.add(new FDLObjectIdentifier("fixed"));
        chain_fixed.add(new FDLObjectIdentifier("true"));
        list_attr.add(chain_fixed);
      }

      chain.add(list_attr);

      final FDLObjectBraceList list_nodes = new FDLObjectBraceList();

      final boolean any = outputNodes(ContextMananger.getNodeManager(), clazz, node_type,
          list_nodes);

      chain.add(list_nodes);

      if (any) {
        uni.add(chain);
        some = true;
      }
    }

    return some;
  }

  private boolean outputLinkClasses(FDLObjectBraceList links) {
    final int clazz_number = this.node_manager.clazz_factory.array.size();
    boolean some = false;
    for (int c = 0; c < clazz_number; c++) {
      final Clazz clazz = (Clazz) this.node_manager.clazz_factory.array
          .elementAt(c);
      final FDLObjectChain chain = new FDLObjectChain(":");
      chain.add(new FDLObjectIdentifier("class"));
      final FDLObjectBracketList list_attr = new FDLObjectBracketList();

      outputColour(clazz, list_attr);

      chain.add(list_attr);

      final FDLObjectBraceList list_links = new FDLObjectBraceList();

      final boolean any = outputLinkTypes(list_links, clazz);

      chain.add(list_links);

      if (any) {
        links.add(chain);
        some = true;
      }
    }

    return some;
  }

  private boolean outputLinkTypes(final FDLObjectBraceList uni, Clazz clazz) {
    final LinkManager link_manager = ContextMananger.getLinkManager();
    final int link_type_number = link_manager.link_type_factory.array.size();
    boolean some = false;

    for (int lt = 0; lt < link_type_number; lt++) {
      final LinkType link_type = (LinkType) link_manager.link_type_factory.array
          .elementAt(lt);
      final FDLObjectChain chain = new FDLObjectChain(":");

      chain.add(new FDLObjectIdentifier("type"));

      final FDLObjectBracketList list_attr = new FDLObjectBracketList();

      final FDLObjectChain chain_length = new FDLObjectChain("=");
      chain_length.add(new FDLObjectIdentifier("length"));
      chain_length.add(new FDLObjectNumber("" + scale(link_type.length)));
      list_attr.add(chain_length);

      final FDLObjectChain chain_radius = new FDLObjectChain("=");
      chain_radius.add(new FDLObjectIdentifier("radius"));
      chain_radius.add(new FDLObjectNumber("" + scale(link_type.radius)));
      list_attr.add(chain_radius);

      final FDLObjectChain chain_elasticity = new FDLObjectChain("=");
      chain_elasticity.add(new FDLObjectIdentifier("elasticity"));
      chain_elasticity
          .add(new FDLObjectNumber("" + scale(link_type.elasticity)));
      list_attr.add(chain_elasticity);

      if (link_type.damping != LinkType.default_damping) {
        final FDLObjectChain chain_damping = new FDLObjectChain("=");
        chain_damping.add(new FDLObjectIdentifier("damping"));
        chain_damping.add(new FDLObjectNumber("" + scale(link_type.damping)));
        list_attr.add(chain_damping);
      }

      if (link_type.hidden) {
        final FDLObjectChain chain_hidden = new FDLObjectChain("=");
        chain_hidden.add(new FDLObjectIdentifier("hidden"));
        chain_hidden.add(new FDLObjectIdentifier("true"));
        list_attr.add(chain_hidden);
      }

      if (link_type.tension) {
        final FDLObjectChain chain_cable = new FDLObjectChain("=");
        chain_cable.add(new FDLObjectIdentifier("tension"));
        chain_cable.add(new FDLObjectIdentifier("true"));
        list_attr.add(chain_cable);
      }

      if (link_type.compression) {
        final FDLObjectChain chain_cable = new FDLObjectChain("=");
        chain_cable.add(new FDLObjectIdentifier("compression"));
        chain_cable.add(new FDLObjectIdentifier("true"));
        list_attr.add(chain_cable);
      }

      if (link_type.disabled) {
        final FDLObjectChain chain_disabled = new FDLObjectChain("=");
        chain_disabled.add(new FDLObjectIdentifier("disabled"));
        chain_disabled.add(new FDLObjectIdentifier("true"));
        list_attr.add(chain_disabled);
      }

      chain.add(list_attr);

      final FDLObjectBraceList list_links = new FDLObjectBraceList();

      final boolean any = outputLinks(link_manager, clazz, link_type,
          list_links);

      chain.add(list_links);

      if (any) {
        uni.add(chain);
        some = true;
      }
    }

    return some;
  }

  private boolean outputFaceClasses(FDLObjectBraceList polygons) {
    final int clazz_number = this.node_manager.clazz_factory.array.size();
    boolean some = false;
    for (int c = 0; c < clazz_number; c++) {
      final Clazz clazz = (Clazz) this.node_manager.clazz_factory.array
          .elementAt(c); //TODO NODE?

      final FDLObjectChain chain = new FDLObjectChain(":");
      chain.add(new FDLObjectIdentifier("class"));
      final FDLObjectBracketList list_attr = new FDLObjectBracketList();

      outputColour(clazz, list_attr);

      chain.add(list_attr);

      final FDLObjectBraceList list_faces = new FDLObjectBraceList();

      final boolean any = outputFaceTypes(list_faces, clazz);

      chain.add(list_faces);

      if (any) {
        polygons.add(chain);
        some = true;
      }
    }
    return some;
  }

  private boolean outputFaceTypes(FDLObjectBraceList uni, Clazz clazz) {
    boolean some = false;
    final FaceManager face_manager = ContextMananger.getFaceManager();
    final int type_number = face_manager.face_type_factory.array.size();

    for (int t = 0; t < type_number; t++) {
      final FaceType polygon_type = (FaceType) face_manager.face_type_factory.array
          .elementAt(t);

      final FDLObjectChain chain = new FDLObjectChain(":");

      chain.add(new FDLObjectIdentifier("type"));

      final FDLObjectBracketList list_attr = new FDLObjectBracketList();

      if (polygon_type.hidden) {
        final FDLObjectChain chain_hidden = new FDLObjectChain("=");
        chain_hidden.add(new FDLObjectIdentifier("hidden"));
        chain_hidden.add(new FDLObjectIdentifier("true"));
        list_attr.add(chain_hidden);
      }

      chain.add(list_attr);
      
      final FDLObjectBracketList faces = new FDLObjectBracketList();

      final boolean any = outputFace(face_manager, clazz, polygon_type, faces);

      chain.add(faces);
      
      if (any) {
        uni.add(chain);
        some = true;
      }
    }

    return some;
  }

  private boolean outputFace(FaceManager face_manager, Clazz clazz,
      FaceType face_type, FDLObjectBracketList faces) {
    boolean some = false;
    final int n_o_l = face_manager.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Face face = (Face) face_manager.element.elementAt(temp);
      if (face.clazz == clazz) {
        if (face.type == face_type) {
          final FDLObjectChain tag_face = outputFace(face);
          faces.add(tag_face);
          some = true;
        }
      }
    }

    return some;
  }

  private FDLObjectChain outputFace(Face face) {
    //final FDLWriterSinglet tag_face = new FDLWriterSinglet("face");

    final FDLObjectChain chain = new FDLObjectChain("=");
    chain.add(new FDLObjectIdentifier("face"));

    final FDLObjectBracketList list = new FDLObjectBracketList();
    
    final int number = face.nodes.size();

    for (int i = 0; i < number; i++) {
      final Node node = (Node) face.nodes.elementAt(i);

      final int n1 = this.nodes.indexOf(node);

      list.add(new FDLObjectNumber("" + n1));
    }

    chain.add(list);

    return chain;
  }

  private boolean outputLinks(LinkManager link_manager, Clazz clazz,
      LinkType link_type, FDLObjectBraceList list_links) {
    boolean some = false;
    final int n_o_l = link_manager.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link link = (Link) link_manager.element.elementAt(temp);
      if (link.clazz == clazz) {
        if (link.type == link_type) {
          final FDLObjectChain tag_node = outputLink(link);
          list_links.add(tag_node);
          some = true;
        }
      }
    }

    return some;
  }

  private FDLObjectChain outputLink(Link link) {
    final FDLObjectChain chain = new FDLObjectChain("=");
    chain.add(new FDLObjectIdentifier("link"));

    final FDLObjectBracketList list = new FDLObjectBracketList();

    final int total = link.nodes.length;
    for (int i = 0; i < total; i++) {
      final Node node = link.nodes[i];
      final int idx = this.nodes.indexOf(node);
      list.add(new FDLObjectNumber("" + idx));
    }

    chain.add(list);

    return chain;
  }

  private boolean outputNodes(NodeManager node_manager, Clazz clazz,
      NodeType node_type, FDLObjectBraceList list_nodes) {
    boolean some = false;
    final int number_of_nodes = node_manager.element.size();
    for (int n = 0; n < number_of_nodes; n++) {
      final Node node = (Node) node_manager.element.elementAt(n);
      if (node.clazz == clazz) {
        if (node.type == node_type) {
          final FDLObjectChain tag_node = outputNode(node);

          list_nodes.add(tag_node);
          this.nodes.addElement(node);
          some = true;
        }
      }
    }

    return some;
  }

  private FDLObjectChain outputNode(final Node node) {
    final FDLObjectChain chain = new FDLObjectChain("=");
    chain.add(new FDLObjectIdentifier("node"));

    final FDLObjectBraceList list = new FDLObjectBraceList();
    list.add(getNodePosition(node));
    final FDLObjectChain chain_velocity = getVelocityPosition(node);
    if (chain_velocity != null) {
      list.add(chain_velocity);
    }

    chain.add(list);
    return chain;
  }

  private FDLObjectChain getNodePosition(final Node node) {
    final FDLObjectChain chain = new FDLObjectChain("=");
    chain.add(new FDLObjectIdentifier("position"));
    final FDLObjectBracketList list = new FDLObjectBracketList();

    final int y = Coords.getInternalFromPixelCoords(Coords.z_pixels)
        - node.pos.z;
    list.add(new FDLObjectNumber("" + scale(node.pos.x)));
    list.add(new FDLObjectNumber("" + y));
    list.add(new FDLObjectNumber("" + scale(node.pos.z)));

    chain.add(list);

    return chain;
  }

  private FDLObjectChain getVelocityPosition(final Node node) {
    if ((node.velocity.x != 0) && (node.velocity.y != 0)
        && (node.velocity.z != 0)) {
      final FDLObjectChain chain = new FDLObjectChain("=");
      chain.add(new FDLObjectIdentifier("velocity"));
      final FDLObjectBracketList list = new FDLObjectBracketList();

      list.add(new FDLObjectNumber("" + scale(node.velocity.x)));
      list.add(new FDLObjectNumber("" + -scale(node.velocity.z)));
      list.add(new FDLObjectNumber("" + scale(node.pos.y)));

      chain.add(list);
      return chain;
    }

    return null;
  }

  int scale(int v) {
    if (v >= 0) {
      return (v + (scale_factor >> 1)) / scale_factor;
    }

    return (v - (scale_factor >> 1)) / scale_factor;
  }

  void writeOut(String s) {
    try {
      if (s.equals("")) {
        if (FrEnd.output_linefeeds) {
          this.out.write("\n");
        }
      } else {
        this.out.write(s + " ");
      }
    } catch (IOException e) {
      Log.log("Error (writeOut): " + e.toString());
    }
  }
}