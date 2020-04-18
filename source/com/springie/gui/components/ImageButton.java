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
   * Create a new button with the given images, group, and initial state.
   */
  public ImageButton(ImageWrapper upImage, ImageWrapper downImage,
      ImageButtonGroup group, String name, boolean state) {

    this.button_image_up = upImage;
    this.button_image_down = downImage;
    this.group = group;
    this.name = name;
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
    Forget.about(x);
    Forget.about(y);

    this.pointer_over = true;
    repaint();
    return true;
  }

  public boolean mouseExit(Event evt, int x, int y) {
    Forget.about(evt);
    Forget.about(x);
    Forget.about(y);

    this.pointer_over = false;

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
}
