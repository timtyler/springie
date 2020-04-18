// This code has been placed into the public domain by its author

package com.springie.gui.components;

/**
 * The ImageButtonGroup object allows a collection of ImageButtons to function
 * as a radio group. The ImageButtonGroup object is essentially the same as the
 * java.awt.ButtonGroup object, but works with ImageButtons instead of
 * java.awt.Button objects.
 */
public class ImageButtonGroup {
  /**
   * The current choice.
   */
  ImageButton current;

  /**
   * Creates a new ImageButtonGroup.
   */
  public ImageButtonGroup() {
    // ...
  }

  /**
   * Gets the current choice.
   */
  public ImageButton getCurrent() {
    return this.current;
  }

  /**
   * Sets the current choice to the specified ImageButton. If the ImageButton
   * belongs to a different group, just return.
   * 
   * @param button
   *          the current ImageButton choice
   */
  public synchronized void setCurrent(ImageButton button) {
    if (button == null) {
      return;
    }

    if (button.group != this) {
      return;
    }

    final ImageButton oldChoice = this.current;
    
    this.current = button;
    if (oldChoice != button) {
      if (oldChoice != null) {
        oldChoice.setStateInternal(false);
        //return;
      }

      if (!button.getState()) {
        button.setStateInternal(true);
        //return;
      }
    }
  }

  /**
   * Returns the String representation of this ImageButtonGroup's values.
   * Convert to String.
   */
  //public String toString() {
    //return getClass().getName() + "[current=" + this.current + "]";
  //}
}
