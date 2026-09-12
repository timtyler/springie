// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import com.springie.FrEnd;
import com.springie.constants.Quality;
import com.springie.gui.components.TTChoice;
import com.springie.messages.MessageManager;
import com.springie.render.RendererDelegator;
import com.springie.render.modules.modern.ModularRendererNew;
import com.springie.render.modules.raytraced.ModularRendererRaytraced;
import com.tifsoft.Forget;

public class PanelPreferencesDisplay {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  public Panel panel_renderer = FrEnd.setUpPanelForFrame();

  public Panel panel_frame = new Panel();

  public Panel panel_main = FrEnd.setUpPanelForFrame();

  MessageManager message_manager;

  // The Display type dropdown appears twice: once at the top of the
  // shared Renderer tab (modern and ray-traced renderers) and once in
  // the original renderer's own Renderer tab, so the renderer can
  // always be switched back. The copies stay in sync; Choice.select()
  // does not fire item events, so syncing never recurses.
  private final List<TTChoice> display_type_choices = new ArrayList<>();

  private TTChoice choose_antialiasing;

  public Label label_fps_value;

  public PanelPreferencesDisplay(MessageManager message_manager) {
    this.message_manager = message_manager;
    makePanel();
  }

  /**
   * Builds one Display type row: the three renderer options. Every copy
   * shares the same listener, which applies the newly chosen renderer
   * and keeps the copies in sync.
   */
  private Panel makeDisplayTypePanel() {
    final Panel panel = new Panel();
    panel.add(new Label("Display type"));

    final TTChoice choice = new TTChoice(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        PanelPreferencesDisplay.this.onDisplayTypeChanged((String) e.getItem());
      }
    });

    choice.add("Modern renderer   ", Quality.SOLID);
    choice.add("Ray-traced renderer", Quality.RAYTRACED);
    choice.add("Original renderer ", Quality.THICK_OUTLINE);
    choice.choice.select(choice.num_to_str(Quality.SOLID));

    panel.add(choice.choice);
    this.display_type_choices.add(choice);
    return panel;
  }

  /**
   * Applies the newly chosen renderer and keeps every Display type
   * copy showing the same choice. The first registered copy serves as
   * the string-to-value prototype (all copies hold identical items);
   * it always exists here because item events only fire on user
   * interaction, long after construction. Choice.select() does not
   * fire item events, so the sync cannot recurse.
   */
  private void onDisplayTypeChanged(String selected) {
    final TTChoice proto = this.display_type_choices.get(0);
    final int value = proto.str_to_num(selected);
    for (final TTChoice c : this.display_type_choices) {
      c.choice.select(c.num_to_str(value));
    }
    applyRendererType(value);
  }

  void makePanel() {
    // The three renderer options live under the Renderer tab now: one
    // copy at the top of the shared Renderer tab (modern and ray-traced
    // renderers), one in the original renderer's own Renderer tab, so
    // the renderer can always be switched back whichever is showing.
    // Both panels exist already: they are built before this one.
    FrEnd.panel_preferences_shared_show.panel.add(makeDisplayTypePanel(), 0);
    FrEnd.panel_preferences_renderer_original.panel_renderer_tab
        .add(makeDisplayTypePanel());

    final Panel panel_antialiasing = getAntiAliasingPanel();

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


    // The Display panel is the frames-per-second readout, the
    // anti-aliasing choice, and the renderer tab bar below.
    this.panel.add(this.panel_renderer);
    
    this.panel_frame.setLayout(new BorderLayout());

    // Frames-per-second readout, kept next to the renderer options so it
    // is visible while tuning the rendering settings.
    final Panel panel_fps = new Panel();
    panel_fps.add(new Label("Frames per second:", Label.RIGHT));
    this.label_fps_value = new Label("X.XXXX", Label.LEFT);
    panel_fps.add(this.label_fps_value);

    final Panel panel_north_top = new Panel(new GridLayout(0, 1));
    panel_north_top.add(panel_antialiasing);
    panel_north_top.add(panel_fps);

    // The shared Renderer/Misc options are tabs in the renderer tab bar
    // below (combined with Options/Filtering/Colours to save space).
    final Panel panel_north = new Panel(new BorderLayout());
    panel_north.add(panel_north_top, "North");

    this.panel_frame.add(panel_north, "North");

    this.panel_frame.add(this.panel_main, "Center");

    // Ray-traced-only options, shown under the shared renderer options
    // while the ray-traced renderer is active. The modern renderer is
    // the default, so the strip starts hidden.
    this.panel_frame.add(FrEnd.panel_preferences_renderer_raytraced.panel,
        "South");
    FrEnd.panel_preferences_renderer_raytraced.panel.setVisible(false);

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

  private Panel getAntiAliasingPanel() {
    final Panel panel = new Panel();
    panel.add(new Label("Anti-aliasing:", Label.RIGHT));

    this.choose_antialiasing = new TTChoice(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        final String scs = (String) e.getItem();
        RendererDelegator.antialiasing =
            PanelPreferencesDisplay.this.choose_antialiasing.str_to_num(scs);
        // The cached tiles are the wrong resolution now.
        FrEnd.main_canvas.forceResize();
      }
    });

    this.choose_antialiasing.add("1x1", 1);
    this.choose_antialiasing.add("2x2", 2);
    this.choose_antialiasing.add("3x3", 3);
    this.choose_antialiasing.choice
        .select(this.choose_antialiasing.num_to_str(1));
    panel.add(this.choose_antialiasing.choice);

    return panel;
  }

  private void applyRendererType(int value) {
    this.panel_main.removeAll();
    final boolean raytraced = value == Quality.RAYTRACED;
    FrEnd.panel_preferences_renderer_raytraced.panel.setVisible(raytraced);
    if (value == Quality.THICK_OUTLINE) {
      RendererDelegator.renderer = new com.springie.render.modules.original.ModularRendererOld();
      this.panel_main.add(FrEnd.panel_preferences_renderer_original.panel,
          "Center");
    } else if (raytraced) {
      RendererDelegator.renderer = new ModularRendererRaytraced();
      // Shares the modern panel: the bin size lives there, and the
      // ray-traced renderer uses the same bins as the default renderer.
      this.panel_main.add(FrEnd.panel_preferences_renderer_modern.panel,
          "Center");
    } else {
      RendererDelegator.renderer = new ModularRendererNew();
      this.panel_main.add(FrEnd.panel_preferences_renderer_modern.panel,
          "Center");
    }
    this.panel_main.validate();
    FrEnd.main_canvas.forceResize();
  }

  /**
   * Restores the default renderer (Modern) and resets every renderer
   * preference panel.
   */
  public void resetToDefaults() {
    // In case it was already selected (no item event fires then),
    // select the default on every copy before applying.
    for (final TTChoice c : this.display_type_choices) {
      c.choice.select(c.num_to_str(Quality.SOLID));
    }
    applyRendererType(Quality.SOLID);

    // Anti-aliasing: 1x1 is off.
    RendererDelegator.antialiasing = 1;
    this.choose_antialiasing.choice
        .select(this.choose_antialiasing.num_to_str(1));

    FrEnd.panel_preferences_renderer_original.resetToDefaults();
    FrEnd.panel_preferences_renderer_modern.resetToDefaults();
    FrEnd.panel_preferences_renderer_raytraced.resetToDefaults();
    FrEnd.panel_preferences_renderer_modern_filters.resetToDefaults();
    FrEnd.panel_preferences_renderer_modern_colours.resetToDefaults();
    FrEnd.panel_preferences_shared_show.resetToDefaults();
    FrEnd.panel_preferences_shared_misc.resetToDefaults();
  }
}
