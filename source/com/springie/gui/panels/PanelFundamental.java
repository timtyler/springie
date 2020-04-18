// This program has been placed into the public domain by its author.

package com.springie.gui.panels;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.Enumeration;

import org.xml.sax.SAXException;

import com.springie.FrEnd;
import com.springie.gui.components.ButtonBar;
import com.springie.gui.components.ChoiceWithDescription;
import com.springie.gui.components.ImageButton;
import com.springie.gui.components.TextFieldWrapper;
import com.springie.gui.panels.preferences.ButtonMouseActionStrings;
import com.springie.messages.Message;
import com.springie.messages.MessageManager;
import com.springie.messages.NewMessage;
import com.springie.messages.NewMessageManager;
import com.springie.presets.AddXMLModelIndexLeaves;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;
import com.tifsoft.Forget;

public class PanelFundamental {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  MessageManager message_manager;

  NewMessageManager new_message_manager;

  public TextFieldWrapper textfield_generate_tube_length;

  public TextFieldWrapper textfield_generate_matrix_x;

  public TextFieldWrapper textfield_generate_matrix_y;

  public TextFieldWrapper textfield_generate_matrix_z;

  public TextFieldWrapper textfield_generate_tube_circumference;

  public TextFieldWrapper textfield_generate_free_nodes;

  public TextFieldWrapper textfield_generate_sphere_pack;

  public ImageButton button_paused;

  public ImageButton button_delete;

  public ImageButton button_select_all_of_class;

  public PanelFundamental(MessageManager message_manager,
      NewMessageManager new_message_manager) {
    this.message_manager = message_manager;
    this.new_message_manager = new_message_manager;
    makePanelGenerate();
    // Insets i = panel.insets();

    // panel.b
  }

  void makePanelGenerate() {
    initialSetup();
    // this.panel.setLayout(new GridLayout(1, 0, 0, 0));
    this.panel.setLayout(new FlowLayout());

    makePanelMouseActions(this.panel);
    this.panel.add(makePanelControls());
    this.panel.add(makePanelPause());
    this.panel.add(makePanelStep());
    this.panel.add(makePanelDelete());
    this.panel.add(makePanelSelectAllOfClass());
    this.panel.add(makePanelZoomIn()); 
    this.panel.add(makePanelZoomOut()); 
    this.panel.add(makePanelPresetIndex());
    this.panel.add(makePanelInitialCvonfiguration());
    this.panel.add(makePanelRestart());
  }

  private Panel makePanelControls() {
    final Panel panel = new Panel();
    panel.setLayout(new BorderLayout(0, 0));
    // panel.setLayout(new GridLayout(1, 1, 0, 0));

    final ImageButton button_controls_edit = new ImageButton("controls", null,
        "Controls", false);

    button_controls_edit.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        Forget.about(arg0);
        FrEnd.frame_controls.setVisible(true);
      }
    });

    panel.add(button_controls_edit);
    return panel;
  }

  // private Panel makePanelPreferences() {
  // final Panel panel_controls_preferences = new Panel();
  // final Button button_controls_preferences = new Button(
  // GUIStrings.CONTROLS_PREFERENCES);
  // button_controls_preferences.addActionListener(new ActionListener() {
  // public void actionPerformed(ActionEvent e) {
  // Forget.about(e);
  // FrEnd.frame_preferences.setVisible(true);
  // }
  // });
  // panel_controls_preferences.add(button_controls_preferences);
  // return panel_controls_preferences;
  // }

  // private Panel makePanelControls() {
  // final Panel panel_controls_edit = new Panel();
  // final Button button_controls_edit = new Button(GUIStrings.CONTROLS_EDIT);
  // button_controls_edit.addActionListener(new ActionListener() {
  // public void actionPerformed(ActionEvent e) {
  // Forget.about(e);
  // FrEnd.frame_controls.setVisible(true);
  // }
  // });
  // panel_controls_edit.add(button_controls_edit);
  // return panel_controls_edit;
  // }

  private Panel makePanelPresetIndex() {
    final Panel panel_preset_index = new Panel();
    panel_preset_index.add(FrEnd.choose_preset_index.choice);
    return panel_preset_index;
  }

  private Panel makePanelInitialCvonfiguration() {
    final Panel panel_initial_configuration = new Panel();
    panel_initial_configuration.add(FrEnd.choose_initial.choice);
    return panel_initial_configuration;
  }

  private Panel makePanelPause() {
    // pause...
    final Panel panel = new Panel();
    panel.setLayout(new GridLayout(1, 1, 0, 0));

    this.button_paused = new ImageButton("pause", null, "Pause", false);
    this.button_paused.setRadio(true);

    this.button_paused.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        Forget.about(arg0);
        getNewMessageManager().add(FrEnd.system_messages.getPauseMessage());
      }
    });
    panel.add(this.button_paused);

    return panel;
  }

  private Panel makePanelStep() {
    // pause...
    final Panel panel = new Panel();
    panel.setLayout(new GridLayout(1, 1, 0, 0));

    // Step...
    FrEnd.button_step = new ImageButton("step", null, "Step", false);
    // this.button_paused.setRadio(true);
    // FrEnd.button_step = new Button(GUIStrings.STEP);
    FrEnd.button_step.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getNewMessageManager().add(new NewMessage(null) {
          public Object execute() {
            // FrEnd.new_message_manager.add(getRestartMessage());
            FrEnd.paused = false;

            try {
              FrEnd.stepping = Integer.parseInt(FrEnd.textfield_step_size
                  .getText());
              // if (!RendererDelegator.isOldDoubleBuffer()) {
              // FrEnd.stepping++;
              // }
            } catch (RuntimeException e) {
              FrEnd.stepping = 1;
            }

            FrEnd.greyPauseAndRestartIfNeeded();
            // FrEnd.button_step.setLabel(GUIStrings.CANCEL);
            FrEnd.button_step.setEnabled(false);
            return null;
          }
        });
      }
    });

    FrEnd.button_step.setFont(FrEnd.bold_font);
    panel.add(FrEnd.button_step);

    return panel;
  }

  private Panel makePanelRestart() {
    final Panel panel = new Panel();
    // panel.setLayout(new GridLayout(1, 1, 0, 0));

    FrEnd.button_restart = new ImageButton("restart", null, "Restart", false);

    FrEnd.button_restart.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        Forget.about(arg0);
        getNewMessageManager().add(FrEnd.system_messages.getRestartMessage());
      }
    });

    panel.setLayout(new BorderLayout(0, 0));
    panel.add(FrEnd.button_restart);
    return panel;
  }

  private Panel makePanelZoomIn() {
    final Panel panel = new Panel();
    panel.setLayout(new BorderLayout(0, 0));

    final ImageButton button_zoom_in = new ImageButton("zoom_in", null,
        "Zoom in", false);

    button_zoom_in.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        Forget.about(arg0);

        Coords.shift_constant_z -= 20;
        if (Coords.shift_constant_z < 1) {
          Coords.shift_constant_z = 1;
        }
        RendererDelegator.repaintAll();
        FrEnd.panel_preferences_viewpoint.reflectTranslateZ();
      }
    });

    panel.add(button_zoom_in);
    return panel;
  }

  
  private Panel makePanelZoomOut() {
    final Panel panel = new Panel();
    panel.setLayout(new BorderLayout(0, 0));

    final ImageButton button = new ImageButton("zoom_out", null, "Zoom out",
        false);

    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        Forget.about(arg0);

        Coords.shift_constant_z += 20;
        RendererDelegator.repaintAll();
        FrEnd.panel_preferences_viewpoint.reflectTranslateZ();
      }
    });

    panel.add(button);
    return panel;
  }

  private Panel makePanelMouseActions(Panel panel) {

    final ButtonBar bb = new ButtonBar();

    bb.add("select", ButtonMouseActionStrings.action_select);
    bb.add("rotate_xy", ButtonMouseActionStrings.action_rotate_xy);
    bb.add("rotate_z", ButtonMouseActionStrings.action_rotate_z);
    bb.add("translate", ButtonMouseActionStrings.action_translate);
    bb.add("link", ButtonMouseActionStrings.action_link);
    bb.add("kill", ButtonMouseActionStrings.action_kill);

    ItemListener il = new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        final String str = (String) (e.getItem());
        FrEnd.action_left_type = ButtonMouseActionStrings
            .stringToActionNumber(str);
      }
    };

    bb.addItemListener(il);

    bb.select(ButtonMouseActionStrings.action_select);

    panel.add(bb);
    return panel;
  }

  private Panel makePanelDelete() {
    final Panel panel = new Panel();
    panel.setLayout(new FlowLayout());

    this.button_delete = new ImageButton("delete", null, "Delete", false);

    this.button_delete.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        Forget.about(arg0);
        getMessageManager().sendMessage(Message.MSG_DELETE, 0, 0);
      }
    });

    panel.setLayout(new BorderLayout(0, 0));
    panel.add(this.button_delete);
    return panel;
  }

  private Panel makePanelSelectAllOfClass() {
    final Panel panel = new Panel();
    panel.setLayout(new FlowLayout());

    this.button_select_all_of_class = new ImageButton("select_all_of_class",
        null, "Select all of class", false);

    this.button_select_all_of_class.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        Forget.about(arg0);
        getMessageManager().sendMessage(Message.MSG_SELECT_CLAZZ, 0, 0);
      }
    });

    panel.setLayout(new BorderLayout(0, 0));
    panel.add(this.button_select_all_of_class);
    return panel;
  }

  private void initialSetup() {
    setUpPresetIndex();

    try {
      new AddXMLModelIndexLeaves().addLeaves(FrEnd.choose_preset_index,
          FrEnd.model_index);
    } catch (IOException e1) {
      e1.printStackTrace();
    } catch (SAXException e1) {
      e1.printStackTrace();
    }

    setUpInitialChoice();
  }

  private void setUpInitialChoice() {
    FrEnd.choose_initial = new ChoiceWithDescription(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        if (e == null) {
          return;
        }

        final String string = (String) (e.getItem());

        FrEnd.next_file_path = (String) FrEnd.choose_initial.hashtable
            .get(string);
      }
    });

    final String index_name = (String) FrEnd.choose_preset_index.hashtable
        .get("Presets");

    setUpLeafIndex(index_name);

    FrEnd.choose_initial.choice.addKeyListener(new KeyListener() {
      public void keyTyped(KeyEvent arg0) {
        Forget.about(arg0);
      }

      public void keyPressed(KeyEvent arg0) {
        if (arg0.getKeyCode() == KeyEvent.VK_ENTER) {
          getMessageManager().sendMessage(Message.MSG_PRESET_CHOSEN, 0, 0);
        }
      }

      public void keyReleased(KeyEvent arg0) {
        Forget.about(arg0);
      }
    });
  }

  private void setUpPresetIndex() {
    FrEnd.choose_preset_index = new ChoiceWithDescription(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        if (e == null) {
          return;
        }

        final String string = (String) (e.getItem());
        final String path = (String) FrEnd.choose_preset_index.hashtable
            .get(string);

        setUpLeafIndex(path);
      }
    });
  }

  private void setUpLeafIndex(final String path) {
    FrEnd.choose_initial.removeAll();
    try {
      new AddXMLModelIndexLeaves().addLeaves(FrEnd.choose_initial, path);
    } catch (IOException e1) {
      e1.printStackTrace();
    } catch (SAXException e1) {
      e1.printStackTrace();
    }
  }

  public static String getXMLIndexPath() {
    final Enumeration e = FrEnd.choose_preset_index.hashtable.keys();
    final String initial = (String) e.nextElement();
    return initial;
  }

  public MessageManager getMessageManager() {
    return this.message_manager;
  }

  public NewMessageManager getNewMessageManager() {
    return this.new_message_manager;
  }
}