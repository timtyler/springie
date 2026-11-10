// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import com.springie.world.World;

/**
 * The Preferences tab's panels, constructed once and shared through
 * these statics. They lived on FrEnd until the preferences module
 * split (2026-11-09): the panels cross-assemble (PanelPreferences
 * builds its tabs out of its siblings' panels, the display panel
 * reads the renderer panels), so they need a common home inside
 * this module, and FrEnd -- which sits below it -- reaches the few
 * operations it needs through FrEnd.PreferencesProvider instead.
 *
 * Construction order matters and is unchanged from FrEnd: the
 * container (panel_preferences) is built last, out of the others.
 */
public final class PreferencesPanels {

  public static PanelPreferencesViewpoint panel_preferences_viewpoint = new PanelPreferencesViewpoint();

  public static PanelPreferencesStereo3D panel_preferences_stereo3d = new PanelPreferencesStereo3D();

  public static PanelPreferencesRendererSharedShow panel_preferences_shared_show = new PanelPreferencesRendererSharedShow();

  public static PanelPreferencesRendererSharedMisc panel_preferences_shared_misc = new PanelPreferencesRendererSharedMisc();

  public static PanelPreferencesEdit panel_preferences_edit = new PanelPreferencesEdit();

  public static PanelPreferencesUpdate panel_preferences_update = new PanelPreferencesUpdate();

  public static PanelPreferencesImport panel_preferences_import = new PanelPreferencesImport();

  public static PanelPreferencesPOVExport panel_preferences_pov_export = new PanelPreferencesPOVExport();

  public static PanelPreferencesRendererOriginal panel_preferences_renderer_original = new PanelPreferencesRendererOriginal(World.new_message_manager);

  public static PanelPreferencesRendererModernFilters panel_preferences_renderer_modern_filters = new PanelPreferencesRendererModernFilters();

  public static PanelPreferencesRendererModernColours panel_preferences_renderer_modern_colours = new PanelPreferencesRendererModernColours();

  // Built before the modern panel: the modern panel's Renderer tab
  // shows these ray-traced-only options at its bottom (South) while
  // the ray-traced renderer is active.
  public static PanelPreferencesRendererRaytraced panel_preferences_renderer_raytraced = new PanelPreferencesRendererRaytraced();

  public static PanelPreferencesRendererModern panel_preferences_renderer_modern = new PanelPreferencesRendererModern(World.new_message_manager);

  public static PanelPreferencesDisplay panel_preferences_display = new PanelPreferencesDisplay();

  public static PanelPreferences panel_preferences = new PanelPreferences();

  private PreferencesPanels() {
  }
}
