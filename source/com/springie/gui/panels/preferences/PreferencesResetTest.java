// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.constants.Actions;
import com.springie.constants.Delay;
import com.springie.constants.Quality;
import com.springie.elements.DeepObjectColourCalculator;
import com.springie.elements.faces.Face;
import com.springie.elements.faces.FaceRenderTypes;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkRenderType;
import com.springie.gui.GuiTestSupport;
import com.springie.preferences.Preferences;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;
import com.springie.render.modules.modern.ColourModifier;
import com.springie.render.modules.modern.ElementRendererLink;
import com.springie.render.modules.modern.ModularRendererNew;
import com.springie.render.modules.modern.RendererBinManager;

/**
 * The Preferences panel's Reset button must restore every preference to its
 * default and update the controls to match.
 */
class PreferencesResetTest {

  @Test
  void resetPreferencesRestoresDefaults() throws Exception {
    GuiTestSupport.bootApp();
    try {

      // Scramble a spread of preferences across every tab.
      SwingUtilities.invokeAndWait(() -> {
        FrEnd.render_nodes = false;
        FrEnd.render_hidden_links = true;
        FrEnd.explosions = false;
        FrEnd.redraw_deepest_first = false;
        FrEnd.delay = Delay._DELAY_100;
        FrEnd.action_left_type = Actions.KILL;
        FrEnd.action_middle_type = Actions.KILL;
        FrEnd.action_right_type = Actions.KILL;
        FrEnd.merge = true;
        FrEnd.quality = Quality.SOLID;
        FrEnd.xor = true;

        Coords.shift_constant_x = 12345;
        Coords.shift_constant_y = -999;

        PanelPreferencesIO.import_scale = 5;
        PanelPreferencesIO.pov_view_height = 99;
        PanelPreferencesIO.pov_immersion_depth = 77;
        FrEnd.preferences.map.put(Preferences.key_output_pov_sky, "black");
        FrEnd.preferences.map.put(Preferences.key_output_pov_ground, "rock");
        FrEnd.preferences.map.put(Preferences.key_output_pov_compression,
            "point");

        RendererBinManager.divisor = 300;
        RendererBinManager.show_bins = true;
        RendererBinManager.colour_modifier_filled = ColourModifier.darker;
        RendererBinManager.colour_modifier_wireframe = ColourModifier.natural;
        ElementRendererLink.strut_divisions = 5;
        ElementRendererLink.cable_divisions = 6;
        RendererDelegator.link_sides = 6;
        RendererDelegator.color_background_number = 0xFFFF0000;
        PanelPreferencesRendererModern.render_label_when = 1;

        Link.number_of_strut_render_divisions = 9;
        Link.number_of_cable_render_divisions = 8;
        Link.link_display_struts_type = LinkRenderType.SOLID;
        Link.link_display_cables_type = LinkRenderType.DOTTED;
        Link.link_display_length = Link.LONG;

        Face.face_display_type = FaceRenderTypes.SEGMENTED;
        Face.number_of_render_divisions = 9;

        DeepObjectColourCalculator.factor = 100;
        DeepObjectColourCalculator.depth_is_relative = false;

        FrEnd.controls_stay_on_top = false;
        // Park the main window where the docked controls are sure to fit on
        // its right, then dock.
        FrEnd.frame_main.setLocation(100, 100);
        FrEnd.controls_dock_with_main = true;
        FrEnd.applyControlsWindowOptions();
      });

      // Docking snaps the controls window against the main window.
      final int[] dock_geometry = new int[4];
      SwingUtilities.invokeAndWait(() -> {
        dock_geometry[0] = FrEnd.frame_main.getX();
        dock_geometry[1] = FrEnd.frame_main.getWidth();
        dock_geometry[2] = FrEnd.frame_controls.getX();
        dock_geometry[3] = FrEnd.frame_controls.getWidth();
      });
      assertEquals(dock_geometry[0] + dock_geometry[1], dock_geometry[2],
          "controls window should sit against the main window when docked");

      SwingUtilities.invokeAndWait(
          () -> FrEnd.panel_preferences.resetPreferences());

      // Every static is back at its default...
      assertTrue(FrEnd.render_nodes);
      assertFalse(FrEnd.render_hidden_links);
      assertTrue(FrEnd.explosions);
      assertTrue(FrEnd.redraw_deepest_first);
      assertEquals(Delay._DELAY_2, FrEnd.delay);
      assertEquals(Actions.SELECT, FrEnd.action_left_type);
      assertEquals(Actions.TRANSLATE, FrEnd.action_middle_type);
      assertEquals(Actions.ROTATE, FrEnd.action_right_type);
      assertFalse(FrEnd.merge);
      assertEquals(Quality.THICK_OUTLINE, FrEnd.quality);
      assertFalse(FrEnd.xor);

      assertEquals(0, Coords.shift_constant_x);
      assertEquals(0, Coords.shift_constant_y);
      assertEquals(Coords.shift_shifted - (Coords.shift_shifted >> 2),
          Coords.shift_constant_z);

      assertEquals(94, PanelPreferencesIO.import_scale);
      assertEquals(50, PanelPreferencesIO.pov_view_height);
      assertEquals(0, PanelPreferencesIO.pov_immersion_depth);
      assertEquals("white",
          FrEnd.preferences.map.get(Preferences.key_output_pov_sky));
      assertEquals("none",
          FrEnd.preferences.map.get(Preferences.key_output_pov_ground));
      assertEquals("bulge",
          FrEnd.preferences.map.get(Preferences.key_output_pov_compression));
      assertEquals(Boolean.FALSE, FrEnd.preferences.map.get(
          Preferences.key_update_animation_when_pointer_over));

      assertEquals(340, RendererBinManager.divisor);
      assertFalse(RendererBinManager.show_bins);
      assertEquals(ColourModifier.natural,
          RendererBinManager.colour_modifier_filled);
      assertEquals(ColourModifier.darker,
          RendererBinManager.colour_modifier_wireframe);
      assertEquals(1, ElementRendererLink.strut_divisions);
      assertEquals(1, ElementRendererLink.cable_divisions);
      assertEquals(3, RendererDelegator.link_sides);
      assertEquals(0xFF000000, RendererDelegator.color_background_number);
      assertEquals(2, PanelPreferencesRendererModern.render_label_when);
      assertTrue(RendererDelegator.renderer instanceof ModularRendererNew,
          "reset must restore the Modern renderer");

      assertEquals(2, Link.number_of_strut_render_divisions);
      assertEquals(1, Link.number_of_cable_render_divisions);
      assertEquals(LinkRenderType.MULTIPLE, Link.link_display_struts_type);
      assertEquals(LinkRenderType.MULTIPLE, Link.link_display_cables_type);
      assertEquals(Link.SHORT, Link.link_display_length);

      assertEquals(FaceRenderTypes.CONCENTRIC, Face.face_display_type);
      assertEquals(4, Face.number_of_render_divisions);

      assertEquals(640, DeepObjectColourCalculator.factor);
      assertTrue(DeepObjectColourCalculator.depth_is_relative);

      // ...and the double-buffer preferences are back too.
      assertEquals(Boolean.TRUE, FrEnd.preferences.map
          .get(Preferences.renderer_new_double_buffer));
      assertEquals(Boolean.FALSE, FrEnd.preferences.map
          .get(Preferences.renderer_old_double_buffer));

      // The controls window is back on top of the main window (never
      // system-wide) and undocked.
      assertTrue(FrEnd.controls_stay_on_top);
      assertFalse(FrEnd.controls_dock_with_main);
      assertFalse(FrEnd.frame_controls.isAlwaysOnTop());
      assertTrue(FrEnd.isControlsStayOnTopActive());
    } finally {
      GuiTestSupport.disposeFrames();
    }
  }
}
