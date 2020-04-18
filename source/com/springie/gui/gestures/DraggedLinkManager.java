// This code has been placed into the public domain by its author

package com.springie.gui.gestures;

import com.springie.FrEnd;
import com.springie.context.ContextMananger;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.links.LinkManager;
import com.springie.elements.links.LinkType;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.geometry.Point3D;
import com.springie.modification.delete.DeleteLinks;
import com.springie.render.Coords;

public class DraggedLinkManager {
  public Node pointer_node;
  Node end_node;

  void doLink(int x, int y) {
    if (FrEnd.button_virginity) {
      final Node dragged_node = ContextMananger.getNodeManager().isThereOne(x, y);
      if (dragged_node != null) {

        new DeleteLinks(ContextMananger.getNodeManager()).prepare();
        FrEnd.perform_selection.doSelectNodes(x, y, false); //? TODO: check this...
        this.pointer_node = new Node(new Point3D(x, y, 0), 999,
          ContextMananger.getNodeManager().node_type_factory);
        this.pointer_node.type.hidden = false;

        final LinkType link_type = ContextMananger.getLinkManager().link_type_factory
          .getNew(60, 0);
        link_type.radius = (dragged_node.type.radius) >> 1;
        link_type.hidden = false;
        link_type.compression = true;
        link_type.tension = true;
        link_type.disabled = true;
        final Clazz dragged_clazz = ContextMananger.getNodeManager().clazz_factory.getNew(0xFFF0FFF0);
        ContextMananger.getLinkManager().setLink(dragged_node,
          this.pointer_node, link_type, dragged_clazz);

        FrEnd.postCleanup();
      }
    }

    dragNewLink(x, y);
  }

  // TODO: this.pointer_node never gets deleted...?
  public void terminateLink(int x, int y) {
    if (this.pointer_node != null) {
      final NodeManager node_manager = ContextMananger.getNodeManager();
      final Node dragged_node = node_manager.getSelectedNode();
      if (dragged_node != null) {
        this.end_node = node_manager.isThereOne(x, y);
        if (this.end_node != null) {
          final int temp_distance = node_manager.distanceBetween(
            dragged_node, this.end_node); // in
          // pixels...
          if (temp_distance > 0) {
            final LinkManager link_manager = ContextMananger.getLinkManager();
            link_manager.deleteAllLinksBetween(dragged_node,
              this.end_node);
            final LinkType link_type = link_manager.link_type_factory
              .getNew(temp_distance, 10);
            link_type.radius = (dragged_node.type.radius + this.end_node.type.radius) >> 2;
            link_type.elasticity = 30;
            link_type.hidden = false;
            link_type.disabled = false;

            final Clazz dragged_clazz = node_manager.clazz_factory.getNew(0xFFF0FFF0);
            link_manager.setLink(dragged_node,
              this.end_node, link_type, dragged_clazz);
          }
        }

        FrEnd.killAllLinks(this.pointer_node);
      }
      
      FrEnd.postCleanup();
    }
  }

  void dragNewLink(int x, int y) {
    if (this.pointer_node != null) {
      this.pointer_node.pos.x = Coords.inverseXCoords(x, 0);
      this.pointer_node.pos.y = Coords.inverseYCoords(y, 0);
    }

    FrEnd.postCleanup();
  }
}