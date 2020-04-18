// This program has been placed into the public domain by its author.

package com.springie.gui.panels.controls;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import com.springie.FrEnd;
import com.springie.context.ContextMananger;
import com.springie.gui.GUIStrings;
import com.springie.gui.components.TTChoice;
import com.springie.gui.panels.UpdateEnabledComponents;
import com.springie.messages.Message;
import com.springie.messages.MessageManager;
import com.springie.messages.NewMessage;
import com.springie.messages.NewMessageManager;
import com.tifsoft.Forget;

public class PanelControlsSelectMain {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  MessageManager message_manager;

  NewMessageManager new_message_manager;

  public Button button_select_all_type;

  public Button button_deselect_all_type;

  public Button button_invert_all_type;

  public Button button_select_clazz;

  public Button button_select_type;

  public Checkbox checkbox_select_nodes;

  public Checkbox checkbox_select_links;

  public Checkbox checkbox_select_faces;

  TTChoice action_select_type;

  public int select_type;

  public Button button_select_faces;

  public Button button_select_nodes;

  public Button button_select_links;

  private Panel panel_type_selector;

  public PanelControlsSelectMain(MessageManager message_manager,
      NewMessageManager new_message_manager) {
    this.message_manager = message_manager;
    this.new_message_manager = new_message_manager;
    makePanel();
  }

  void makePanel() {
    final Panel panel_select_all = new Panel();

    final Label label_select_select_label = new Label("Select all:",
        Label.RIGHT);

    panel_select_all.add(label_select_select_label);

    this.button_select_clazz = new Button("class");
    this.button_select_clazz.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getMessageManager().sendMessage(Message.MSG_SELECT_CLAZZ, 0, 0);
      }
    });
    panel_select_all.add(this.button_select_clazz);

    this.button_select_type = new Button("type");
    this.button_select_type.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getMessageManager().sendMessage(Message.MSG_SELECT_TYPE, 0, 0);
      }
    });
    panel_select_all.add(this.button_select_type);

    final Label label_deselect_deselect_label = new Label("Type", Label.RIGHT);
    final Panel panel_type_select_deselect = new Panel();

    panel_type_select_deselect.add(label_deselect_deselect_label);

    this.button_select_all_type = new Button("Select");
    this.button_select_all_type.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getNewMessageManager().add(new NewMessage(null) {
          public Object execute() {
            FrEnd.prepareToModifyAllTypes();
            MessageManager.selectAllOfType();
            FrEnd.postCleanup();
            return null;
          }
        });
      }
    });
    panel_type_select_deselect.add(this.button_select_all_type);

    this.button_deselect_all_type = new Button("Deselect");
    this.button_deselect_all_type.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getNewMessageManager().add(new NewMessage(null) {
          public Object execute() {
            FrEnd.prepareToModifyAllTypes();
            MessageManager.deselectAllOfType();
            FrEnd.postCleanup();
            return null;
          }
        });
      }
    });

    panel_type_select_deselect.add(this.button_deselect_all_type);

    this.button_invert_all_type = new Button("Invert");
    this.button_invert_all_type.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getNewMessageManager().add(new NewMessage(null) {
          public Object execute() {
            FrEnd.prepareToModifyAllTypes();
            MessageManager.invertAllOfType();
            FrEnd.postCleanup();
            return null;
          }
        });
      }
    });

    panel_type_select_deselect.add(this.button_invert_all_type);

    this.panel_type_selector = getPanelTypeSelector();

    this.panel.add(this.panel_type_selector);
    this.panel.add(panel_type_select_deselect);
    this.panel.add(panel_select_all);
    this.panel.add(getSelectAllPanel());
  }

  private Panel getPanelTypeSelector() {
    this.panel_type_selector = new Panel();

    resetPanelTypeSelector(true, true, true);

    return this.panel_type_selector;
  }

  public void resetPanelTypeSelector(boolean nodes, boolean links,
      boolean faces) {
    this.panel_type_selector.removeAll();

    final boolean any = nodes || links || faces;
    if (any) {
      final Label label_selection_label = new Label("Select:", Label.RIGHT);
      this.panel_type_selector.add(label_selection_label);
    }

    if (nodes) {
      this.checkbox_select_nodes = new Checkbox(GUIStrings.NODES, true);
      this.checkbox_select_nodes.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
          Forget.about(e);
          UpdateEnabledComponents.greySelectButtonsDependingOnSelection();
        }
      });
      this.panel_type_selector.add(this.checkbox_select_nodes);
    }

    if (links) {
      this.checkbox_select_links = new Checkbox(GUIStrings.LINKS, true);
      this.checkbox_select_links.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
          Forget.about(e);
          UpdateEnabledComponents.greySelectButtonsDependingOnSelection();
        }
      });
      this.panel_type_selector.add(this.checkbox_select_links);
    }
    
    if (faces) {
      this.checkbox_select_faces = new Checkbox(GUIStrings.FACES, true);
      this.checkbox_select_faces.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
          Forget.about(e);
          UpdateEnabledComponents.greySelectButtonsDependingOnSelection();
        }
      });
      this.panel_type_selector.add(this.checkbox_select_faces);
    }
    this.panel_type_selector.validate();
  }

  private Panel getSelectAllPanel() {
    this.action_select_type = new TTChoice(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        if (e == null) {
          return;
        }

        final String stateChangedString = (String) (e.getItem());

        if (PanelControlsSelectMain.this.action_select_type
            .str_to_num(stateChangedString) != -99) {
          PanelControlsSelectMain.this.select_type = PanelControlsSelectMain.this.action_select_type
              .str_to_num(stateChangedString);
        }
      }
    });

    this.action_select_type.add("Select", 0);
    this.action_select_type.add("Deselect", 1);
    this.action_select_type.add("Invert", 2);
    this.action_select_type.choice.select(this.action_select_type
        .num_to_str(this.select_type));

    // choice with invert here...

    final Panel panel_select_all = new Panel();
    panel_select_all.add(this.action_select_type.choice);

    // panel_select_all.add(label_select_select_label);

    this.button_select_nodes = new Button("nodes");
    this.button_select_nodes.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getNewMessageManager().add(new NewMessage(null) {
          public Object execute() {
            FrEnd.prepareToModifyNodeTypes();
            switch (PanelControlsSelectMain.this.select_type) {
              case 0:
                ContextMananger.getNodeManager().selectAll();
                break;
              case 1:
                ContextMananger.getNodeManager().deselectAll();
                break;
              case 2:
                ContextMananger.getNodeManager().selectionInvert();
                break;
              default:
                break;
            }

            FrEnd.postCleanup();
            return null;
          }
        });
      }
    });
    panel_select_all.add(this.button_select_nodes);

    this.button_select_links = new Button("links");
    this.button_select_links.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getNewMessageManager().add(new NewMessage(null) {
          public Object execute() {
            FrEnd.prepareToModifyLinkTypes();
            switch (PanelControlsSelectMain.this.select_type) {
              case 0:
                ContextMananger.getLinkManager().selectAll();
                break;
              case 1:
                ContextMananger.getLinkManager().deselectAll();
                break;
              case 2:
                ContextMananger.getLinkManager().selectionInvert();
                break;
              default:
                break;
            }
            FrEnd.postCleanup();
            return null;
          }
        });
      }
    });
    panel_select_all.add(this.button_select_links);

    this.button_select_faces = new Button("faces");
    this.button_select_faces.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getNewMessageManager().add(new NewMessage(null) {
          public Object execute() {
            FrEnd.prepareToModifyFaceTypes();
            switch (PanelControlsSelectMain.this.select_type) {
              case 0:
                ContextMananger.getFaceManager().selectAll();
                break;
              case 1:
                ContextMananger.getFaceManager().deselectAll();
                break;
              case 2:
                ContextMananger.getFaceManager().selectionInvert();
                break;
              default:
                break;
            }
            FrEnd.postCleanup();
            return null;
          }
        });
      }
    });
    panel_select_all.add(this.button_select_faces);

    return panel_select_all;
  }

  public MessageManager getMessageManager() {
    return this.message_manager;
  }

  public NewMessageManager getNewMessageManager() {
    return this.new_message_manager;
  }
}
