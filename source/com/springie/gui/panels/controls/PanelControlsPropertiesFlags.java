// This program has been placed into the public domain by its author.

package com.springie.gui.panels.controls;

import java.awt.Checkbox;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import com.springie.FrEnd;
import com.springie.context.ContextMananger;
import com.springie.elements.base.BaseElement;
import com.springie.elements.faces.FaceManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.Message;
import com.springie.messages.MessageManager;
import com.tifsoft.Forget;

public class PanelControlsPropertiesFlags {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  MessageManager message_manager;

  public Checkbox checkbox_pinned;

  public Checkbox checkbox_hidden;

  public Checkbox checkbox_compression;

  public Checkbox checkbox_tension;

  public Checkbox checkbox_disabled;

  public PanelControlsPropertiesFlags(MessageManager message_manager) {
    this.message_manager = message_manager;
    resetPanel(false, false, false);
  }

  public void resetPanel(boolean nodes, boolean links, boolean faces) {
    final Panel panel_edit_checkboxes_1 = new Panel();
    this.checkbox_hidden = new Checkbox("Hidden", anySelectedThingHidden());
    this.checkbox_hidden.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        getMessageManager().sendMessage(Message.MSG_EDIT_HIDE_FLAG, 0, 0);
      }
    });
    if (nodes || links || faces) {
      panel_edit_checkboxes_1.add(this.checkbox_hidden);
    }

    this.checkbox_pinned = new Checkbox("Pinned", anySelectedNodesPinned());
    this.checkbox_pinned.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        getMessageManager().sendMessage(Message.MSG_EDIT_FIXED_FLAG, 0, 0);
      }
    });
    if (nodes) {
      panel_edit_checkboxes_1.add(this.checkbox_pinned);
    }

    this.checkbox_disabled = new Checkbox("Disabled",
        anySelectedLinksDisabled());
    this.checkbox_disabled.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        getMessageManager().sendMessage(Message.MSG_EDIT_DISABLED_FLAG, 0, 0);
      }
    });
    if (links) {
      panel_edit_checkboxes_1.add(this.checkbox_disabled);
    }

    if (!links && !nodes && !faces) {
      panel_edit_checkboxes_1.add(new Label(""));
    }

    final Panel panel_edit_checkboxes_links = new Panel();

    this.checkbox_compression = new Checkbox("Compression",
        anySelectedLinksSustainCompression());
    this.checkbox_compression.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        getMessageManager().sendMessage(Message.MSG_EDIT_ROPE_FLAG, 0, 0);
      }
    });

    if (links) {
      panel_edit_checkboxes_links.add(this.checkbox_compression);
    }

    this.checkbox_tension = new Checkbox("Tension",
        anySelectedLinksSustainTension());
    this.checkbox_tension.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        getMessageManager().sendMessage(Message.MSG_EDIT_ROPE_FLAG, 0, 0);
      }
    });
    if (links) {
      panel_edit_checkboxes_links.add(this.checkbox_tension);
    } else {
      panel_edit_checkboxes_links.add(new Label(""));
    }
    this.panel.removeAll();

    this.panel.add(panel_edit_checkboxes_1);
    this.panel.add(panel_edit_checkboxes_links);

    this.panel.validate();
  }

  private boolean anySelectedLinksSustainCompression() {
    if (ContextMananger.getNodeManager() == null) {
      return false;
    }
    final LinkManager link_manager = ContextMananger.getLinkManager();
    final int size = link_manager.element.size();
    for (int i = 0; i < size; i++) {
      final Link l = (Link) link_manager.element.get(i);
      if (l.type.selected) {
        if (l.type.compression) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean anySelectedLinksSustainTension() {
    if (ContextMananger.getNodeManager() == null) {
      return false;
    }
    final LinkManager link_manager = ContextMananger.getLinkManager();
    final int size = link_manager.element.size();
    for (int i = 0; i < size; i++) {
      final Link l = (Link) link_manager.element.get(i);
      if (l.type.selected) {
        if (l.type.tension) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean anySelectedLinksDisabled() {
    if (ContextMananger.getNodeManager() == null) {
      return false;
    }
    final LinkManager link_manager = ContextMananger.getLinkManager();
    final int size = link_manager.element.size();
    for (int i = 0; i < size; i++) {
      final Link l = (Link) link_manager.element.get(i);
      if (l.type.selected) {
        if (l.type.disabled) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean anySelectedNodesPinned() {
    if (ContextMananger.getNodeManager() == null) {
      return false;
    }
    final NodeManager node_manager = ContextMananger.getNodeManager();
    final int size = node_manager.element.size();
    for (int i = 0; i < size; i++) {
      final Node n = (Node) node_manager.element.get(i);
      if (n.type.selected) {
        if (n.type.pinned) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean anySelectedThingHidden() {
    if (ContextMananger.getNodeManager() == null) {
      return false;
    }
    
    final NodeManager node_manager = ContextMananger.getNodeManager();
    final LinkManager link_manager = ContextMananger.getLinkManager();
    final FaceManager face_manager = ContextMananger.getFaceManager();

    return anySelectedThingHidden(node_manager.element)
        || anySelectedThingHidden(link_manager.element)
        || anySelectedThingHidden(face_manager.element);
  }

  private boolean anySelectedThingHidden(List v) {
    // final NodeManager node_manager = ContextMananger.getNodeManager();
    final int size = v.size();
    for (int i = 0; i < size; i++) {
      final BaseElement n = (BaseElement) v.get(i);
      if (n.isSelected()) {
        if (n.isHidden()) {
          return true;
        }
      }
    }
    return false;
  }

  public MessageManager getMessageManager() {
    return this.message_manager;
  }
}
