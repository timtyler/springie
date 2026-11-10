// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import com.springie.FrEnd;
import java.awt.Panel;

/**
 * The FrEnd.PreferencesProvider implementation: hands the GUI the
 * Preferences tab's root panel and the three operations it performs
 * on the panels, delegating to the shared instances on
 * PreferencesPanels. Discovered by FrEnd via ServiceLoader (see
 * META-INF/services), the way PresetObjectsProvider serves World.
 */
public class PreferencesPanelsProvider implements FrEnd.PreferencesProvider {

  /** ServiceLoader instantiation. */
  public PreferencesPanelsProvider() {
  }

  @Override
  public Panel panel() {
    return PreferencesPanels.panel_preferences.panel;
  }

  @Override
  public void reflectTranslateZ() {
    PreferencesPanels.panel_preferences_viewpoint.reflectTranslateZ();
  }

  @Override
  public void setBottomToolbarCheckbox(final boolean state) {
    PreferencesPanels.panel_preferences.checkbox_display_bottom_toolbar
        .setState(state);
  }

  @Override
  public void setFpsLabelText(final String text) {
    PreferencesPanels.panel_preferences_display.label_fps_value
        .setText(text);
  }
}
