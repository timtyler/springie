// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import com.springie.FrEnd;
import com.springie.elements.DeepObjectColourCalculator;
import com.springie.elements.faces.Face;
import com.springie.gui.GUIStrings;
import com.springie.messages.MessageManager;
import com.springie.render.RendererDelegator;
import com.tifsoft.Forget;

public class PanelPreferencesRendererSharedMisc {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  public Panel panel_shared = FrEnd.setUpPanelForFrame();

  MessageManager message_manager;

  public Checkbox checkbox_explosions;

  public Checkbox checkbox_redraw_deepest_first;

  private Label label_face_render_number;

  public Checkbox checkbox_relative_fog;

  private Label label_fog;

  //public Label label_fps_value;

  public PanelPreferencesRendererSharedMisc(MessageManager message_manager) {
    this.message_manager = message_manager;
    makePanel();
  }

  void makePanel() {

//    final TTChoice choose_display_struts = new TTChoice(new ItemListener() {
//      public void itemStateChanged(ItemEvent e) {
//        final String scs = (String) e.getItem();
//        Link.link_display_struts_type = FrEnd.choose_display_struts
//            .str_to_num(scs);
//        RendererDelegator.repaintAll();
//      }
//    });
//
//    FrEnd.choose_display_struts = choose_display_struts;

//    final Panel panel_bin_size = getBinSizePanel();
    final Panel panel_fog = getFogPanel();

    final Panel panel_visible_explosions = new Panel();
    this.checkbox_explosions = new Checkbox(GUIStrings.EXPLOSIONS);
    this.checkbox_explosions.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        FrEnd.explosions = ((Checkbox) e.getSource()).getState();
      }
    });
    this.checkbox_explosions.setState(FrEnd.explosions);
    panel_visible_explosions.add(this.checkbox_explosions);

    final Panel panel_redraw_deepest_first = new Panel();
    this.checkbox_redraw_deepest_first = new Checkbox(
        "Render deepest objects first");
    this.checkbox_redraw_deepest_first.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        FrEnd.redraw_deepest_first = ((Checkbox) e.getSource()).getState();
      }
    });
    this.checkbox_redraw_deepest_first.setState(FrEnd.redraw_deepest_first);
    panel_redraw_deepest_first.add(this.checkbox_redraw_deepest_first);
    
    final Panel panel_relative_fog = new Panel();
    this.checkbox_relative_fog = new Checkbox(
        "Fog depth is relative");
    this.checkbox_relative_fog.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        DeepObjectColourCalculator.depth_is_relative = ((Checkbox) e.getSource()).getState();
      }
    });
    this.checkbox_relative_fog.setState(DeepObjectColourCalculator.depth_is_relative);
    panel_relative_fog.add(this.checkbox_relative_fog);
    
    final Panel panel_face_render_number = getFaceRenderNumber();

    this.panel.add(panel_redraw_deepest_first);
    this.panel.add(panel_visible_explosions);
    this.panel.add(panel_relative_fog);
    this.panel.add(panel_fog);
    this.panel.add(panel_face_render_number);
  }


  private Panel getFogPanel() {
    final Panel panel = new Panel();
    panel.setLayout(new BorderLayout(0, 8));
    panel.add("West", new Label("Fog:", Label.RIGHT));

    final Scrollbar scroll_bar_fog = new Scrollbar(Scrollbar.HORIZONTAL,
        DeepObjectColourCalculator.factor / 10, 10, 0, 110);
    scroll_bar_fog.addAdjustmentListener(new AdjustmentListener() {
      public void adjustmentValueChanged(AdjustmentEvent e) {
        final int temp = e.getValue();
        DeepObjectColourCalculator.factor = temp * 10;
        reflectLabelFog();
      }
    });

    panel.add("Center", scroll_bar_fog);

    this.label_fog = new Label("", Label.LEFT);
    panel.add("East", this.label_fog);
    reflectLabelFog();

    return panel;
  }

  private Panel getFaceRenderNumber() {
    final Panel panel = new Panel();
    panel.setLayout(new BorderLayout(0, 8));
    panel.add("West", new Label("Face lines:", Label.RIGHT));

    final Scrollbar scroll_bar_face_render_number = new Scrollbar(
        Scrollbar.HORIZONTAL, Face.number_of_render_divisions, 4, 0, 28);
    scroll_bar_face_render_number
        .addAdjustmentListener(new AdjustmentListener() {
          public void adjustmentValueChanged(AdjustmentEvent e) {
            final int temp = e.getValue();
            Face.number_of_render_divisions = temp;
            reflectLabelFaceRenderNumber();
            RendererDelegator.repaintAll();
          }
        });

    panel.add("Center", scroll_bar_face_render_number);

    this.label_face_render_number = new Label("", Label.LEFT);
    panel.add("East", this.label_face_render_number);
    reflectLabelFaceRenderNumber();

    return panel;
  }

  private void reflectLabelFog() {
    getLabelFog().setText("" + (DeepObjectColourCalculator.factor / 10));
  }

  public Label getLabelFog() {
    return this.label_fog;
  }

  private void reflectLabelFaceRenderNumber() {
    getLabelFaceRenderNumber().setText("" + Face.number_of_render_divisions);
  }
  public Label getLabelFaceRenderNumber() {
    return this.label_face_render_number;
  }

  public MessageManager getMessageManager() {
    return this.message_manager;
  }
}
