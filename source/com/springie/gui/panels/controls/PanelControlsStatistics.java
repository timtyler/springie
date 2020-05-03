// This program has been placed into the public domain by its author.

package com.springie.gui.panels.controls;

import java.awt.Component;
import java.awt.Label;
import java.awt.Panel;

import com.springie.FrEnd;
import com.springie.context.ContextMananger;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.io.out.WriteFloatingPoint;
import com.springie.messages.MessageManager;

public class PanelControlsStatistics {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  MessageManager message_manager;

  public Label label_main_statistics;

  public Label label_link_length_stats;

  public int number_of_nodes;

  public int number_of_links;

  public int number_of_faces;

  public int number_of_selected_nodes;

  public int number_of_selected_links;

  public int number_of_selected_faces;

  public static int length_of_shortest_link;

  public PanelControlsStatistics(MessageManager message_manager) {
    this.message_manager = message_manager;
    makePanel();
    updateGUIToReflectSelectionChange();
  }

  void makePanel() {
    this.panel.add(makePanelNumberOfSelected());
    this.panel.add(makePanelLinkLengthStats());
  }

  private Component makePanelNumberOfSelected() {
    // final Panel panel = new Panel();
    this.label_main_statistics = new Label("XX", Label.CENTER);
    // panel.add(this.label_main_statistics);

    return this.label_main_statistics;
  }

  private Component makePanelLinkLengthStats() {
    //final Panel panel = new Panel();
    //final Label label_link_length_stats = new Label("Link rest length:", Label.RIGHT);
    //panel.add(label_link_length_stats);
    this.label_link_length_stats = new Label("XX", Label.LEFT);
    //panel.add(this.label_link_length_stats);

    return this.label_link_length_stats;
  }

  public void updateGUIToReflectPropertiesChange() {
    final NodeManager node_manager = ContextMananger.getNodeManager();
    if (node_manager == null) {
      return;
    }

    final LinkManager link_manager = ContextMananger.getLinkManager();

    length_of_shortest_link = Integer.MAX_VALUE;
    long total = 0;
    int count = 0;

    final int n_o_l = link_manager.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link link = (Link) link_manager.element.get(temp);
      if (link.type.length < length_of_shortest_link) {
        length_of_shortest_link = link.type.length;
      }
      if (link.type.selected) {
        total += link.type.length;
        count++;
      }
    }

    String text = "";
    if (count > 0) {
      if (length_of_shortest_link > 0) {
        final long average = total / count;
        final double fraction = average / (double) length_of_shortest_link;

        text += "Link rest length: ";
        text += WriteFloatingPoint.emit((float) fraction, 5, true);
      }
    }

    this.label_link_length_stats.setText(text);
  }

  public void updateGUIToReflectSelectionChange() {
    final NodeManager node_manager = ContextMananger.getNodeManager();
    if (node_manager == null) {
      return;
    }

    this.number_of_nodes = node_manager.element.size();
    this.number_of_links = node_manager.getLinkManager().element.size();
    this.number_of_faces = node_manager.getFaceManager().element.size();

    this.number_of_selected_nodes = node_manager.getNumberOfSelected();
    this.number_of_selected_links = node_manager.getLinkManager()
        .getNumberOfSelected();
    this.number_of_selected_faces = node_manager.getFaceManager()
        .getNumberOfSelected();

    String text = "Nodes: " + this.number_of_nodes + " ("
        + this.number_of_selected_nodes + ")";
    text += "  Links: " + this.number_of_links + " ("
        + this.number_of_selected_links + ")";
    text += "  Faces: " + this.number_of_faces + " ("
        + this.number_of_selected_faces + ")";

    this.label_main_statistics.setText(text);

    updateGUIToReflectPropertiesChange();
  }
}
