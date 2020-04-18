// This program has been placed into the public domain by its author.

package com.springie.gui.panels.statistics;

import java.awt.Label;
import java.awt.Panel;

import com.springie.FrEnd;
import com.springie.messages.MessageManager;

public class PanelStatistics {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  public Panel panel_shared = FrEnd.setUpPanelForFrame();

  MessageManager message_manager;

  public Label label_fps_value;

  //public Label label_number_of_nodes;

  //public Label label_number_of_links;
  
  //public Label label_number_of_faces;

  //public Label label_number_of_nodes_selected;

  //public Label label_number_of_links_selected;
  
  //public Label label_number_of_faces_selected;

  public PanelStatistics(MessageManager message_manager) {
    this.message_manager = message_manager;
    makePanel();
  }

  void makePanel() {
    //this.panel.add(makePanelNumberOfNodes());
    //this.panel.add(makePanelNumberOfLinks());
    //this.panel.add(makePanelNumberOfFaces());
    this.panel.add(makePanelFPS());
  }

//  private Panel makePanelNumberOfNodes() {
//    final Panel panel = new Panel();
//    final Label label_number_of_nodes = new Label("# nodes:", Label.RIGHT);
//    panel.add(label_number_of_nodes);
//    this.label_number_of_nodes = new Label("XXX", Label.LEFT);
//    panel.add(this.label_number_of_nodes);
//    
//    final Label label_number_of_nodes_selected = new Label("Selected:", Label.RIGHT);
//    panel.add(label_number_of_nodes_selected);
//    this.label_number_of_nodes_selected = new Label("XXX", Label.LEFT);
//    panel.add(this.label_number_of_nodes_selected);
//
//    return panel;
//  }
//
//  private Panel makePanelNumberOfLinks() {
//    final Panel panel = new Panel();
//    final Label label_number_of_links = new Label("# links:", Label.RIGHT);
//    panel.add(label_number_of_links);
//    this.label_number_of_links = new Label("XXX", Label.LEFT);
//    panel.add(this.label_number_of_links);
//    
//    final Label label_number_of_links_selected = new Label("Selected:", Label.RIGHT);
//    panel.add(label_number_of_links_selected);
//    this.label_number_of_links_selected = new Label("XXX", Label.LEFT);
//    panel.add(this.label_number_of_links_selected);
//
//    return panel;
//  }
//
//  private Panel makePanelNumberOfFaces() {
//    final Panel panel = new Panel();
//    final Label label_number_of_faces = new Label("# faces:", Label.RIGHT);
//    panel.add(label_number_of_faces);
//    this.label_number_of_faces = new Label("XXX", Label.LEFT);
//    panel.add(this.label_number_of_faces);
//    
//    final Label label_number_of_faces_selected = new Label("Selected:", Label.RIGHT);
//    panel.add(label_number_of_faces_selected);
//    this.label_number_of_faces_selected = new Label("XXX", Label.LEFT);
//    panel.add(this.label_number_of_faces_selected);
//
//    return panel;
//  }

  private Panel makePanelFPS() {
    final Panel panel = new Panel();
    final Label label_fps_1 = new Label("Frames per second:", Label.RIGHT);
    panel.add(label_fps_1);
    this.label_fps_value = new Label("X.XXXX", Label.LEFT);
    panel.add(this.label_fps_value);
    return panel;
  }
}
