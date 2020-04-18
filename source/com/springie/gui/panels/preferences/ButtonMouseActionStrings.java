// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import com.springie.constants.Actions;


public final class ButtonMouseActionStrings {
  public static String action_select = "Select";
  public static String action_translate = "Translate";
  public static String action_rotate_xy = "Rotate(X&Y)";
  public static String action_rotate_z = "Rotate(Z)";
  public static String action_link = "Link";
  public static String action_kill = "Kill";
  
  
  private ButtonMouseActionStrings() {
    //...
  }
  
  public static int stringToActionNumber(String str) {
    if (str == ButtonMouseActionStrings.action_select) {
      return Actions.SELECT;
    } else    if (str == ButtonMouseActionStrings.action_rotate_xy) {
      return Actions.ROTATE;
    } else    if (str == ButtonMouseActionStrings.action_rotate_z) {
      return Actions.ROTATE_CW_ACW;
    } else    if (str == ButtonMouseActionStrings.action_translate) {
      return Actions.TRANSLATE;
    } else    if (str == ButtonMouseActionStrings.action_kill) {
      return Actions.KILL;
    } else    if (str == ButtonMouseActionStrings.action_link) {
      return Actions.LINK;
    }

    return -1;
  }
}