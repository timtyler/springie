package com.springie.context;

import com.springie.elements.faces.FaceManager;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.NodeManager;

public final class ContextMananger {
  public static NodeManager node_manager;

  private ContextMananger() {
    //...
  }
  //public static LinkManager link_manager;

  //public static FaceManager face_manager;

  public static NodeManager getNodeManager() {
    return node_manager;
  }

  public static LinkManager getLinkManager() {
    return node_manager.getLinkManager();
    //return link_manager;
  }

  public static FaceManager getFaceManager() {
    return node_manager.getFaceManager();
    //return face_manager;
  }

  public static void setNodeManager(NodeManager node_manager) {
    ContextMananger.node_manager = node_manager;
  }
}
