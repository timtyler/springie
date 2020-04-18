// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import java.awt.BorderLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import com.springie.FrEnd;
import com.springie.constants.Quality;
import com.springie.gui.components.TTChoice;
import com.springie.gui.components.TabbedPanel;
import com.springie.messages.MessageManager;
import com.springie.render.RendererDelegator;
import com.springie.render.modules.modern.ModularRendererNew;

public class PanelPreferencesDisplay {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  public Panel panel_renderer = FrEnd.setUpPanelForFrame();

  public Panel panel_shared = FrEnd.setUpPanelForFrame();

  public Panel panel_frame = new Panel();

  public Panel panel_main = FrEnd.setUpPanelForFrame();

  MessageManager message_manager;

  private TTChoice choose_display_type;

  public PanelPreferencesDisplay(MessageManager message_manager) {
    this.message_manager = message_manager;
    makePanel();
  }

  void makePanel() {
    final Panel panel_type = new Panel();
    final Label label_type_1 = new Label("Display type");

    this.choose_display_type = new TTChoice(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        final String scs = (String) e.getItem();
        final int value = PanelPreferencesDisplay.this.choose_display_type
            .str_to_num(scs);
        PanelPreferencesDisplay.this.panel_main.removeAll();
        if (value == Quality.THICK_OUTLINE) {
          RendererDelegator.renderer = new com.springie.render.modules.original.ModularRendererOld();
          PanelPreferencesDisplay.this.panel_main.add(
              FrEnd.panel_preferences_renderer_original.panel, "Center");
        } else {
          RendererDelegator.renderer = new ModularRendererNew();
          PanelPreferencesDisplay.this.panel_main.add(
              FrEnd.panel_preferences_renderer_modern.panel, "Center");
        }
        PanelPreferencesDisplay.this.panel_main.validate();
        FrEnd.main_canvas.forceResize();
      }
    });

    this.choose_display_type.add("Modern renderer   ", Quality.SOLID);
    this.choose_display_type.add("Original renderer ", Quality.THICK_OUTLINE);
    this.choose_display_type.choice.select(this.choose_display_type
        .num_to_str(Quality.SOLID));

    panel_type.add(label_type_1);
    panel_type.add(this.choose_display_type.choice);

    // ...


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


    final TabbedPanel tab = new TabbedPanel();
    tab.add("Renderer", this.panel_renderer);
    
    tab.add("Shared",  FrEnd.panel_preferences_shared.panel);

    this.panel.add(tab);
    
    this.panel_frame.setLayout(new BorderLayout());

    this.panel_frame.add(panel_type, "North");

    this.panel_frame.add(this.panel_main, "Center");

    this.panel_main.add(FrEnd.panel_preferences_renderer_modern.panel, "Center");

    this.panel_renderer.setLayout(new BorderLayout());

    this.panel_renderer.add(this.panel_frame, "Center");

    // Common preferences...

//    this.panel_shared.add(panel_render_normal);
//    this.panel_shared.add(panel_render_hidden);
//    this.panel_shared.add(panel_redraw_deepest_first);
//    this.panel_shared.add(panel_fog);
//    this.panel_shared.add(panel_visible_explosions);
//    if (FrEnd.development_version) {
//      this.panel_shared.add(panel_fps);
//    }
  }

   public MessageManager getMessageManager() {
    return this.message_manager;
  }
}
