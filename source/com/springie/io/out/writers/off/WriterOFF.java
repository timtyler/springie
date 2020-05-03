package com.springie.io.out.writers.off;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Vector;

import com.springie.elements.faces.Face;
import com.springie.elements.faces.FaceManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.geometry.Point3D;
import com.springie.io.out.GarbageCollection;
import com.springie.io.out.WriteFloatingPoint;
import com.springie.metrics.BoundingBox;
import com.springie.modification.redundancy.RedundancyRemover;
import com.springie.utilities.log.Log;

public class WriterOFF {
  float scale_factor;

  BoundingBox bb;

  Point3D middle = new Point3D(0, 0, 0);

  Writer out;

  Vector nodes;

  private NodeManager node_manager;

  private LinkManager link_manager;

  private FaceManager face_manager;

  public WriterOFF(NodeManager node_manager) {
    this.node_manager = node_manager;
    this.link_manager = node_manager.getLinkManager();
    this.face_manager = node_manager.getFaceManager();
  }

  public void write(String filename) {
    new GarbageCollection(this.node_manager).cleanUp();
    new RedundancyRemover(this.node_manager).removeRedundancy();

    this.bb = new BoundingBox();
    this.bb.find(this.node_manager);

    final float sfx = 2f / (this.bb.max.x - this.bb.min.x);
    final float sfy = 2f / (this.bb.max.y - this.bb.min.y);
    final float sfz = 2f / (this.bb.max.z - this.bb.min.z);
    this.scale_factor = Math.min(sfx, Math.min(sfy, sfz));

    this.middle.x = (this.bb.max.x + this.bb.min.x) >> 1;
    this.middle.y = (this.bb.max.y + this.bb.min.y) >> 1;
    this.middle.z = (this.bb.max.z + this.bb.min.z) >> 1;

    this.nodes = new Vector();

    try {
      try {
        this.out = new FileWriter(filename);
        writeHeader();

        final int n_nodes = this.node_manager.element.size();

        final int n_links = this.link_manager.element.size();

        final int n_faces = this.face_manager.element.size();

        writeLine("" + n_nodes + " " + (n_links + n_faces) + " " + n_links);
        writeLine("");
        //writeLine("# Nodes"); // other programs barf on this...
        outputNodes();
        if (n_faces > 0) {
          writeLine("");
          //writeLine("# Faces:" + n_faces); // other programs barf on this...
          outputFaces();
        }
        if (n_links > 0) {
          writeLine("");
          //writeLine("# Links:" + n_links); // other programs barf on this...
          outputLinks();
        }

      } finally {
        this.out.flush();
        this.out.close();
      }
    } catch (IOException e) {
      Log.log("Error in write: " + e);
    }
  }

  private void writeHeader() {
    writeLine("OFF");
  }

  private void outputNodes() {
    final int number_of_nodes = this.node_manager.element.size();
    for (int n = 0; n < number_of_nodes; n++) {
      final Node node = (Node) this.node_manager.element.get(n);
      outputNode(node);
    }
  }

  private void outputNode(final Node node) {
    final float x = toVRMLCoords(node.pos.x - this.middle.x);
    final float y = toVRMLCoords(node.pos.y - this.middle.y);
    final float z = toVRMLCoords(node.pos.z - this.middle.z);

    writeLine("" + emit(x) + " " + emit(y) + " " + emit(z));
  }

  private void outputLinks() {
    final int n_o_l = this.link_manager.element.size();

    for (int temp = n_o_l; --temp >= 0;) {
      final Link link = (Link) this.link_manager.element.get(temp);
      outputLink(link);
    }
  }

  private void outputLink(Link link) {
    final Node node1 = link.nodes[0];
    final Node node2 = link.nodes[1];

    final int i1 = this.node_manager.element.indexOf(node1);
    final int i2 = this.node_manager.element.indexOf(node2);

    writeLine("2 " + i1 + " " + i2);
  }

  private void outputFaces() {
    final int n_o_l = this.face_manager.element.size();

    for (int temp = n_o_l; --temp >= 0;) {
      final Face face = (Face) this.face_manager.element.get(temp);
      outputFace(face);
    }
  }

  private void outputFace(Face face) {
    final int n = face.nodes.size();
    final StringBuffer out = new StringBuffer();
    out.append("" + n);
    for (int i = 0; i < n; i++) {
      final Node node = (Node) face.nodes.get(i);
      out.append(" ");
      final int index = this.node_manager.element.indexOf(node);
      out.append("" + index);
    }

    writeLine(out.toString());
  }

  float toVRMLCoords(int v) {
    return v * this.scale_factor;
  }

  public String emit(float f) {
    return WriteFloatingPoint.emit(f, 5, false);
  }

  void writeLine(String s) {
    try {
      this.out.write(s + "\n");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}