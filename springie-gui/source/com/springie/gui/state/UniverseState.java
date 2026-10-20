package com.springie.gui.state;

import com.springie.constants.Actions;
import com.springie.constants.Delay;
import com.springie.constants.ToolTypes;

/**
 * Mutable application state, extracted from FrEnd (Tim, 2026-10-14).
 * The eventual aim is for all mutable state to live in dedicated
 * classes like this one, not as FrEnd statics.
 */
public final class UniverseState {
  private UniverseState() {
  }

  // Controls window mode.
  public static final int CONTROLS_ALWAYS_ON_TOP = 0;
  public static final int CONTROLS_FREE_FLOATING = 1;
  public static final int CONTROLS_DOCKED = 2;
  public static int controls_window_mode = CONTROLS_DOCKED;

  // Current tool.
  public static int weapon_type = ToolTypes._PENCIL;

  // Animation delay.
  public static int delay = Delay._DELAY_10;

  // Mouse button actions.
  public static int action_left_type = Actions.SELECT;
  public static int action_middle_type = Actions.TRANSLATE;
  public static int action_right_type = Actions.ROTATE;

  /** Restores factory defaults (Tim, 2026-10-14). */
  public static void resetToDefaults() {
    controls_window_mode = CONTROLS_DOCKED;
    weapon_type = ToolTypes._PENCIL;
    delay = Delay._DELAY_10;
    action_left_type = Actions.SELECT;
    action_middle_type = Actions.TRANSLATE;
    action_right_type = Actions.ROTATE;
  }
}
