package com.springie.io.out;

import com.springie.context.ContextMananger;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.faces.Face;
import com.springie.elements.faces.FaceManager;
import com.springie.elements.faces.FaceType;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.links.LinkType;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.elements.nodes.NodeType;

public class AreThereAny {
  public boolean nodes(Clazz clazz) {
    final NodeManager manager = ContextMananger.getNodeManager();
    final int number = manager.element.size();

    for (int i = number; --i >= 0;) {
      final Node node = (Node) manager.element.get(i);
      if (node.clazz.equals(clazz)) {
        return true;
      }
    }
    return false;
  }

  public boolean links(Clazz clazz) {
    final LinkManager manager = ContextMananger.getLinkManager();
    final int number = manager.element.size();

    for (int i = number; --i >= 0;) {
      final Link link = (Link) manager.element.get(i);
      if (link.clazz.equals(clazz)) {
        return true;
      }
    }
    return false;
  }

  public boolean polygons(Clazz clazz) {
    final FaceManager manager = ContextMananger.getFaceManager();
    final int number = manager.element.size();

    for (int i = number; --i >= 0;) {
      final Face polygon = (Face) manager.element.get(i);
      if (polygon.clazz.equals(clazz)) {
        return true;
      }
    }
    return false;
  }

  public boolean nodes(Clazz clazz, NodeType type) {
    final NodeManager manager = ContextMananger.getNodeManager();
    final int number = manager.element.size();

    for (int i = number; --i >= 0;) {
      final Node node = (Node) manager.element.get(i);
      if (node.clazz.equals(clazz)) {
        if (node.type.equals(type)) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean links(Clazz clazz, LinkType type) {
    final LinkManager manager = ContextMananger.getLinkManager();
    final int number = manager.element.size();

    for (int i = number; --i >= 0;) {
      final Link link = (Link) manager.element.get(i);
      if (link.clazz.equals(clazz)) {
        if (link.type.equals(type)) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean polygons(Clazz clazz, FaceType type) {
    final FaceManager manager = ContextMananger.getFaceManager();
    final int number = manager.element.size();

    for (int i = number; --i >= 0;) {
      final Face polygon = (Face) manager.element.get(i);
      if (polygon.clazz.equals(clazz)) {
        if (polygon.type.equals(type)) {
          return true;
        }
      }
    }
    return false;
  }
}
