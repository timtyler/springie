// This program has been placed into the public domain by its author.

package com.springie.gui.panels.controls;

import java.awt.Button;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.springie.FrEnd;
import com.springie.context.ContextMananger;
import com.springie.gui.GUIStrings;
import com.springie.gui.components.TextFieldWrapper;
import com.springie.messages.Message;
import com.springie.messages.MessageManager;
import com.springie.messages.NewMessage;
import com.springie.messages.NewMessageManager;
import com.tifsoft.Forget;

public class PanelControlsSelectAdvanced {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  MessageManager message_manager;
  NewMessageManager new_message_manager;

  public Button button_edit_spread_selection_via_links;

  Button button_select_nodes;

  TextFieldWrapper textfield_select_nodes_with_n_links;

  public TextFieldWrapper textfield_select_faces_with_n_sides;

  public PanelControlsSelectAdvanced(MessageManager message_manager, NewMessageManager new_message_manager) {
    this.message_manager = message_manager;
    this.new_message_manager = new_message_manager;
    makePanel();
  }

  void makePanel() {
    this.panel.add(getSpreadSelectionViaLinksPanel());

    this.panel.add(getSelectAllNodesWithNLinkPanel());
    this.panel.add(getSelectAllFacesWithNSidesPanel());
}

  private Panel getSpreadSelectionViaLinksPanel() {
    this.button_edit_spread_selection_via_links = new Button(
        GUIStrings.SPREAD_SELECTION_VIA_LINKS);
    this.button_edit_spread_selection_via_links
        .addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Forget.about(e);
            getMessageManager().sendMessage(
                Message.MSG_EDIT_SPREAD_SELECTION_VIA_LINKS, 0, 0);
          }
        });
    final Panel panel = new Panel();
    panel.add(this.button_edit_spread_selection_via_links);
    return panel;
  }

  private Panel getSelectAllFacesWithNSidesPanel() {
    final Panel panel = new Panel();
    Button button_select_faces;

    button_select_faces = new Button("Select faces with");
    button_select_faces.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getMessageManager().sendMessage(
          Message.MSG_SELECT_ALL_FACES_WITH_N_SIDES, 0, 0);
      }
    });
    panel.add(button_select_faces);

    this.textfield_select_faces_with_n_sides = new TextFieldWrapper("5");
    panel.add(this.textfield_select_faces_with_n_sides);

    final Label label_select = new Label("sides", Label.LEFT);
    panel.add(label_select);

    return panel;
  }
  
  private Panel getSelectAllNodesWithNLinkPanel() {
    final Panel panel = new Panel();

    this.button_select_nodes = new Button("Select nodes with");
    this.button_select_nodes.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getNewMessageManager().add(new NewMessage(null) {
          public Object execute() {
            FrEnd.prepareToModifyNodeTypes();
            final int n_links = Integer
              .parseInt(FrEnd.panel_edit_select_advanced.textfield_select_nodes_with_n_links
                .getText());
            ContextMananger.getNodeManager().selectAllWithNLinks(n_links);
            FrEnd.postCleanup();
            return null;
          }
        });
      }
    });
    panel.add(this.button_select_nodes);

    this.textfield_select_nodes_with_n_links = new TextFieldWrapper("0");
    panel.add(this.textfield_select_nodes_with_n_links);

    final Label label_select = new Label("links", Label.LEFT);
    panel.add(label_select);

    return panel;
  }

  public MessageManager getMessageManager() {
    return this.message_manager;
  }
  
  public NewMessageManager getNewMessageManager() {
    return this.new_message_manager;
  }
}
