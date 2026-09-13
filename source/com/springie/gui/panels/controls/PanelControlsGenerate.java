// This program has been placed into the public domain by its author.

package com.springie.gui.panels.controls;

import java.awt.Button;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.springie.FrEnd;
import com.springie.gui.GUIStrings;
import com.springie.gui.components.TextFieldWrapper;
import com.springie.messages.NewMessageManager;
import com.springie.messages.commands.GenerateFreeNodesMessage;
import com.springie.messages.commands.GenerateMatrixMessage;
import com.springie.messages.commands.GenerateSpherePackMessage;
import com.springie.messages.commands.GenerateStringMessage;
import com.springie.messages.commands.GenerateTubeMessage;
import com.tifsoft.Forget;

public class PanelControlsGenerate {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  NewMessageManager new_message_manager;

  public TextFieldWrapper textfield_generate_tube_length;

  public TextFieldWrapper textfield_generate_matrix_x;
  
  public TextFieldWrapper textfield_generate_matrix_y;
  
  public TextFieldWrapper textfield_generate_matrix_z;

  public TextFieldWrapper textfield_generate_tube_circumference;

  public TextFieldWrapper textfield_generate_free_nodes;

  public TextFieldWrapper textfield_generate_sphere_pack;

  public TextFieldWrapper textfield_generate_string_length;

  public PanelControlsGenerate(NewMessageManager new_message_manager) {
    this.new_message_manager = new_message_manager;
    makePanelGenerate();
  }

  void makePanelGenerate() {
    final Panel panel_generate_tube = makeTubeGUI();

    final Panel panel_generate_free_nodes = makeFreeNodesGUI();

    final Panel panel_generate_sphere_pack = makeSpherePackGUI();

    final Panel panel_generate_matrix = makeMatrixGUI();

    final Panel panel_generate_string = makeStringGUI();

    final Panel panel_shapes = FrEnd.setUpPanelForFrame2();
    panel_shapes.add(panel_generate_tube);
    panel_shapes.add(panel_generate_matrix);
    panel_shapes.add(panel_generate_sphere_pack);
    panel_shapes.add(panel_generate_string);
    panel_shapes.add(panel_generate_free_nodes);

    this.panel.add(panel_shapes);
  }

  private Panel makeTubeGUI() {
    final Button button = new Button(GUIStrings.GENERATE_TUBE);
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getNewMessageManager().add(new GenerateTubeMessage());
      }
    });

    final Panel panel = new Panel();

    panel.add(new Label("Length:", Label.RIGHT));
    this.textfield_generate_tube_length = new TextFieldWrapper("20");
    panel.add(this.textfield_generate_tube_length);

    panel.add(new Label("D:", Label.RIGHT));
    this.textfield_generate_tube_circumference = new TextFieldWrapper("3");
    panel.add(this.textfield_generate_tube_circumference);

    panel.add(button);
    return panel;
  }

  private Panel makeStringGUI() {
    final Button button = new Button(GUIStrings.GENERATE_STRING);
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getNewMessageManager().add(new GenerateStringMessage());
      }
    });

    final Panel panel = new Panel();

    panel.add(new Label("Length:", Label.RIGHT));
    this.textfield_generate_string_length = new TextFieldWrapper("8");
    panel.add(this.textfield_generate_string_length);

    panel.add(button);
    return panel;
  }

  private Panel makeMatrixGUI() {
    final Button button = new Button(GUIStrings.GENERATE_MATRIX);
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getNewMessageManager().add(new GenerateMatrixMessage());
      }
    });

    final Panel panel = new Panel();

    panel.add(new Label("X:", Label.RIGHT));
    this.textfield_generate_matrix_x = new TextFieldWrapper("6");
    panel.add(this.textfield_generate_matrix_x);

    panel.add(new Label("Y:", Label.RIGHT));
    this.textfield_generate_matrix_y = new TextFieldWrapper("3");
    panel.add(this.textfield_generate_matrix_y);

    panel.add(new Label("Z:", Label.RIGHT));
    this.textfield_generate_matrix_z = new TextFieldWrapper("6");
    panel.add(this.textfield_generate_matrix_z);

    panel.add(button);
    return panel;
  }

  private Panel makeFreeNodesGUI() {
    final Button button = new Button(
      GUIStrings.GENERATE_FREE_NODES);
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getNewMessageManager().add(new GenerateFreeNodesMessage());
      }
    });

    final Panel panel = new Panel();

    panel.add(new Label("N:", Label.RIGHT));
    this.textfield_generate_free_nodes = new TextFieldWrapper("1000");

    panel.add(this.textfield_generate_free_nodes);

    panel.add(button);
    return panel;
  }

  private Panel makeSpherePackGUI() {
    final Button button = new Button(
      GUIStrings.GENERATE_SPHERE_PACK);
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getNewMessageManager().add(new GenerateSpherePackMessage());
      }
    });

    final Panel panel = new Panel();

    panel.add(new Label("N:", Label.RIGHT));
    this.textfield_generate_sphere_pack = new TextFieldWrapper("20");

    panel.add(this.textfield_generate_sphere_pack);

    panel.add(button);
    return panel;
  }

  public NewMessageManager getNewMessageManager() {
    return this.new_message_manager;
  }
}