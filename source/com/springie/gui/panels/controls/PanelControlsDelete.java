// This program has been placed into the public domain by its author.

package com.springie.gui.panels.controls;

import java.awt.Button;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.springie.FrEnd;
import com.springie.context.ContextMananger;
import com.springie.gui.GUIStrings;
import com.springie.messages.Message;
import com.springie.messages.MessageManager;
import com.springie.messages.NewMessage;
import com.springie.messages.NewMessageManager;
import com.springie.modification.links.FuseSelectedNodes;
import com.tifsoft.Forget;

public class PanelControlsDelete {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  MessageManager message_manager;
  NewMessageManager new_message_manager;

  public Button button_delete_selection;

  public Button button_delete_all;

  public Button button_edit_remove_links;

  public Button button_edit_remove_polygons;

  public Button button_edit_remove_link_and_fuse_ends;

  public Button button_edit_fuse_nodes;
  
  public PanelControlsDelete(MessageManager message_manager, NewMessageManager new_message_manager) {
    this.message_manager = message_manager;
    this.new_message_manager = new_message_manager;
    makePanel();
  }

  void makePanel() {
    final Panel panel_delete = new Panel();
    this.button_delete_selection = new Button("Delete selection");
    this.button_delete_selection.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getMessageManager().sendMessage(Message.MSG_DELETE, 0, 0);
      }
    });
    panel_delete.add(this.button_delete_selection);

    this.panel.add(panel_delete);
    this.panel.add(getRemoveLinksPanel());
    this.panel.add(getRemoveFacesPanel());
    this.panel.add(getPanelRemoveLinkAndFuseEnds());
    this.panel.add(getPanelFuseSelectedNodes());
    this.panel.add(getDeleteAllPanel());
  }
  
  private Panel getRemoveFacesPanel() {
    this.button_edit_remove_polygons = new Button(
      GUIStrings.DOME_REMOVE_POLYGONS);
    this.button_edit_remove_polygons.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getMessageManager().sendMessage(Message.MSG_EDIT_REMOVE_POLYGONS, 0, 0);
      }
    });
    final Panel panel = new Panel();
    panel.add(this.button_edit_remove_polygons);
    return panel;
  }

  private Panel getRemoveLinksPanel() {
    this.button_edit_remove_links = new Button(GUIStrings.DOME_REMOVE_LINKS);
    this.button_edit_remove_links.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getMessageManager().sendMessage(Message.MSG_EDIT_REMOVE_LINKS, 0, 0);
      }
    });
    final Panel panel = new Panel();
    panel.add(this.button_edit_remove_links);
    return panel;
  }

  private Panel getDeleteAllPanel() {
	this.button_delete_all = new Button(GUIStrings.BUTTON_CLEAR);
    button_delete_all.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getNewMessageManager().add(new NewMessage(null) {
          public Object execute() {
            ContextMananger.getNodeManager().initialSetUp();

            FrEnd.last_file_path = null;

            FrEnd.reflectValuesInGUIAfterPropertyEditing();
            return null;
          }
        });        
      }
    });
    final Panel panel = new Panel();
    panel.add(button_delete_all);
    return panel;
  }
  
  private Panel getPanelRemoveLinkAndFuseEnds() {
    this.button_edit_remove_link_and_fuse_ends = new Button(
        GUIStrings.EDIT_REMOVE_LINK_AND_FUSE_ENDS);
    this.button_edit_remove_link_and_fuse_ends
        .addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Forget.about(e);
            getMessageManager().sendMessage(
                Message.MSG_EDIT_REMOVE_LINK_AND_FUSE_ENDS, 0, 0);
          }
        });
    final Panel panel_edit_remove_link_and_fuse_ends = new Panel();
    panel_edit_remove_link_and_fuse_ends
        .add(this.button_edit_remove_link_and_fuse_ends);
    return panel_edit_remove_link_and_fuse_ends;
  }

  private Panel getPanelFuseSelectedNodes() {
    this.button_edit_fuse_nodes = new Button(
        GUIStrings.EDIT_FUSE_SELECTED_NODES);
    this.button_edit_fuse_nodes
        .addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Forget.about(e);
            getNewMessageManager().add(new NewMessage(null) {
              public Object execute() {
                //Log.log("EXEXC!");
                final FuseSelectedNodes fsn = new FuseSelectedNodes(
                    ContextMananger.getNodeManager());
                fsn.action();
                return null;
              }
            });
          }
        });
    final Panel panel_edit_fuse_selected_nodes = new Panel();
    panel_edit_fuse_selected_nodes.add(this.button_edit_fuse_nodes);
    return panel_edit_fuse_selected_nodes;
  }

  public MessageManager getMessageManager() {
    return this.message_manager;
  }
  
  public NewMessageManager getNewMessageManager() {
    return this.new_message_manager;
  }
}
