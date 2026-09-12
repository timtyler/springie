// This code has been placed into the public domain by its author

package com.springie.gui.components;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.springie.utilities.ImageLoader;
import com.springie.utilities.ImageWrapper;
import com.tifsoft.Forget;

public class ImageButton extends Canvas implements ActionListener {

  static final long serialVersionUID = 1L;

  ImageWrapper button_image_up;

  ImageWrapper button_image_down;

  ImageWrapper button_image_up_hover;

  ImageWrapper button_image_down_hover;

  ImageWrapper button_image_up_grey;

  ImageWrapper button_image_down_grey;

  boolean button_pressed;

  boolean pointer_over;

  boolean pressed;

  boolean radio;
  
  String name;

  ImageButtonGroup group;

  private ActionListener listener;

  /**
   * Hover help text. AWT has no built-in tooltips, so ImageButton shows
   * its own small popup label after the pointer rests on the button.
   * Defaults to the button's human-readable name; callers can override.
   */
  private String tooltip_text;

  /** Delay before the tooltip appears, in milliseconds. */
  private static final int TOOLTIP_DELAY_MS = 750;

  private static java.util.Timer tooltip_timer;

  private static java.awt.Window tooltip_window;

  private static java.awt.Label tooltip_label;

  /** The button whose tooltip is currently pending or showing. */
  private static ImageButton tooltip_target;

  private int tooltip_mouse_x;

  private int tooltip_mouse_y;

  /**
   * Create a new button with the given images, group, and initial state.
   */
  public ImageButton(ImageWrapper upImage, ImageWrapper downImage,
      ImageButtonGroup group, String name, boolean state) {

    this.button_image_up = upImage;
    this.button_image_down = downImage;
    this.group = group;
    this.name = name;
    this.tooltip_text = name;
    setState(state);
  }

  /**
   * Create a new button with the given images and group, and initially in its
   * "false" (up) state.
   */
  public ImageButton(ImageWrapper upImage, ImageWrapper downImage,
      ImageButtonGroup group, String name) {
    this(upImage, downImage, group, name, false);
  }

  public ImageButton(String iname, ImageButtonGroup group, String name, boolean state) {
    this(ImageLoader.getImage(GraphicsDirectory.directory + iname + "_o.png"),
        ImageLoader.getImage(GraphicsDirectory.directory + iname + "_i.png"),
        group, name, state);
  }

  ImageWrapper getCurentImage() {
    //if (this.button_pressed && this.pointer_over) {
      //return this.button_image_down;
    //}

    return (this.pressed) ? this.button_image_down : this.button_image_up;
  }

  ImageWrapper getHoverImage() {
    setUpHoverImages();
    if (this.button_pressed) {
      return this.button_image_down_hover;
    }

    return (this.pressed) ? this.button_image_down_hover : this.button_image_up_hover;
  }

  ImageWrapper getCurentImageGrey() {
    setUpGreyImages();
    if (this.button_pressed && this.pointer_over) {
      return this.button_image_down_grey;
    }

    return (this.pressed) ? this.button_image_down_grey : this.button_image_up_grey;
  }

  private void setUpGreyImages() {
    if (this.button_image_down_grey == null) {
      this.button_image_down_grey = getGreyImage(this.button_image_down);
      this.button_image_up_grey = getGreyImage(this.button_image_up);
    }
  }

  private void setUpHoverImages() {
    if (this.button_image_down_hover == null) {
      this.button_image_down_hover = getLightenImage(this.button_image_down);
      this.button_image_up_hover = getLightenImage(this.button_image_up);
    }
  }

  public Dimension preferredSize() {
    final Image ib = this.button_image_up.getImage();

    final int width = ib.getWidth(this);
    final int height = ib.getHeight(this);
    
    return new Dimension(width, height);
  }

  public Dimension minimumSize() {
    return preferredSize();
  }

  public void paint(Graphics g) {
    ImageWrapper iw = getCurentImage();
    if (this.pointer_over) {
      iw = getHoverImage();
    }
    
    if (!this.isEnabled()) {
      iw = getCurentImageGrey();
    }
    
    g.drawImage(iw.getImage(), 0, 0, this);
  }

  
  
  private ImageWrapper getLightenImage(ImageWrapper input) {
    return ImageProcessor.hsbFilter(input, 0.9F, -0.9F, -0.5F);
  }

  private ImageWrapper getGreyImage(ImageWrapper input) {
    return ImageProcessor.hsbFilter(input, 1.0F, 0.1F, 0.6F);
  }

  /**
   * Return the state (true corresponds to down) of the button.
   */
  public boolean getState() {
    return this.pressed;
  }

  public void setEnabled(boolean b) {
    super.setEnabled(b);  
    repaint();
  }

  /**
   * Set the state of the button. True corresponds to down.
   */
  public void setState(boolean state) {
    if (this.group != null) {
      this.group.setCurrent(this);
    }

    setStateInternal(state);
  }

  public void setStateInternal(boolean state) {
    if (this.pressed != state) {
      this.pressed = state;
      sendActionEvent(state);
    }

    repaint();
  }

  private void sendActionEvent(boolean state) {
    final ActionEvent ie = new ActionEvent(this, state ? 1 : 0, this.name, 0);
    this.actionPerformed(ie);
  }

  public boolean mouseDown(Event evt, int x, int y) {
    Forget.about(evt);
    Forget.about(x);
    Forget.about(y);
    cancelTooltip();
    this.button_pressed = true;
    repaint();
    return true;
  }

  public boolean mouseUp(Event evt, int x, int y) {
    Forget.about(evt);
    Forget.about(x);
    Forget.about(y);
    if (this.button_pressed && this.pointer_over) {
      // //postEvent(new Event(this, evt.when, Event.ACTION_EVENT, evt.x, evt.y,
      // evt.key, evt.modifiers, evt.arg));
      if (this.radio) {
        if (this.pressed) {
          if (this.group == null) {
            setState(false);
          }
        } else {
          setState(true);
        }
      } else {
        sendActionEvent(getState());
      }
    }
    this.button_pressed = false;
    repaint();
    return true;
  }

  public boolean mouseEnter(Event evt, int x, int y) {
    Forget.about(evt);

    this.pointer_over = true;
    repaint();
    scheduleTooltip(x, y);
    return true;
  }

  public boolean mouseExit(Event evt, int x, int y) {
    Forget.about(evt);
    Forget.about(x);
    Forget.about(y);

    this.pointer_over = false;
    cancelTooltip();

    repaint();
    return true;
  }

  public void addActionListener(ActionListener listener) {
    this.listener = listener;
  }

  public void actionPerformed(ActionEvent arg0) {
    if (this.listener != null) {
      this.listener.actionPerformed(arg0);
    }
  }

  public boolean getRadio() {
    return this.radio;
  }

  public void setRadio(boolean radio) {
    this.radio = radio;
  }

  /**
   * Overrides the default tooltip (the button's name) with custom help
   * text, shown while the pointer hovers over the button.
   */
  public void setTooltipText(String text) {
    this.tooltip_text = text;
  }

  public String getTooltipText() {
    return this.tooltip_text;
  }

  private void scheduleTooltip(int x, int y) {
    cancelTooltip();
    if (this.tooltip_text == null || this.tooltip_text.isEmpty()) {
      return;
    }
    this.tooltip_mouse_x = x;
    this.tooltip_mouse_y = y;
    tooltip_target = this;
    if (tooltip_timer == null) {
      tooltip_timer = new java.util.Timer("ImageButton-tooltips", true);
    }
    final ImageButton target = this;
    tooltip_timer.schedule(new java.util.TimerTask() {
      public void run() {
        java.awt.EventQueue.invokeLater(new Runnable() {
          public void run() {
            showTooltip(target);
          }
        });
      }
    }, TOOLTIP_DELAY_MS);
  }

  private static void cancelTooltip() {
    tooltip_target = null;
    if (tooltip_timer != null) {
      tooltip_timer.cancel();
      tooltip_timer = null;
    }
    if (tooltip_window != null) {
      tooltip_window.setVisible(false);
    }
  }

  private static void showTooltip(ImageButton target) {
    if (tooltip_target != target || !target.pointer_over
        || !target.isEnabled() || !target.isShowing()) {
      return;
    }
    final String text = target.tooltip_text;
    if (text == null || text.isEmpty()) {
      return;
    }
    final java.awt.Point screen;
    try {
      screen = target.getLocationOnScreen();
    } catch (java.awt.IllegalComponentStateException e) {
      Forget.about(e);
      return;
    }
    final java.awt.Window owner = findWindowAncestor(target);
    if (owner == null) {
      return;
    }
    if (tooltip_window == null || tooltip_window.getOwner() != owner) {
      if (tooltip_window != null) {
        tooltip_window.dispose();
      }
      tooltip_window = new java.awt.Window(owner);
      final java.awt.Panel border = new java.awt.Panel() {
        static final long serialVersionUID = 1L;

        public java.awt.Insets getInsets() {
          return new java.awt.Insets(1, 1, 1, 1);
        }
      };
      border.setBackground(java.awt.Color.BLACK);
      border.setLayout(new java.awt.BorderLayout());
      tooltip_label = new java.awt.Label();
      tooltip_label.setBackground(new java.awt.Color(255, 255, 225));
      border.add(tooltip_label, java.awt.BorderLayout.CENTER);
      tooltip_window.add(border);
    }
    if (!text.equals(tooltip_label.getText())) {
      tooltip_label.setText(text);
    }
    tooltip_window.pack();
    final java.awt.Dimension screen_size = java.awt.Toolkit.getDefaultToolkit()
        .getScreenSize();
    final java.awt.Dimension tip_size = tooltip_window.getSize();
    int x = screen.x + target.tooltip_mouse_x + 14;
    int y = screen.y + target.tooltip_mouse_y + 18;
    if (x + tip_size.width > screen_size.width) {
      x = screen_size.width - tip_size.width;
    }
    if (y + tip_size.height > screen_size.height) {
      // No room below the cursor: show it above the button instead.
      y = screen.y + target.tooltip_mouse_y - tip_size.height - 8;
    }
    if (x < 0) {
      x = 0;
    }
    if (y < 0) {
      y = 0;
    }
    tooltip_window.setLocation(x, y);
    tooltip_window.setVisible(true);
  }

  private static java.awt.Window findWindowAncestor(java.awt.Component c) {
    while (c != null && !(c instanceof java.awt.Window)) {
      c = c.getParent();
    }
    return (java.awt.Window) c;
  }
}
