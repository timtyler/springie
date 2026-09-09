package com.springie.elements.nodes;

import java.util.ArrayList;

public class NodeTypeFactory {
  public ArrayList<NodeType> array = new ArrayList<>();

  public NodeType getNew(NodeType current) {
    final NodeType type = new NodeType(current);
    this.array.add(type);
    type.makeEqualTo(current);
    return type;
  }

  public NodeType getNew() {
    final NodeType type = new NodeType();
    this.array.add(type);
    return type;
  }

//  public static NodeType getNew(NodeType type) {
//    // TODO Auto-generated method stub
//    return null;
//  }

//public public NodeTypeFactory(NodeType current) {
//final NodeType type = new NodeType();
//array.add(type);
//type.makeEqualTo(type)
//t
//return type;
//}

}
