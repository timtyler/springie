// This code has been placed into the public domain by its author

package com.springie.render;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.ImageObserver;

import com.springie.FrEnd;
import com.springie.context.ContextMananger;
import com.springie.messages.Message;
import com.springie.messages.NewMessage;

public class MainCanvas {
  static final long serialVersionUID = 1250; 

  public Panel panel = RendererPanels.setUpPanelForFrame2();

  RendererInfoButton info_button = new RendererInfoButton();

  // for crude double-buffering
  Image image_offscreen;

  public Graphics graphics_handle;

  ImageObserver observer;

  FrEnd parent;

  public int modifiers;

  int xoff = 4;

  int yoff = 4;

  int img_x;

  int img_y;

  Dimension dim;

  int old_gridsize = -99;

  int new_gridsize;

  int current_mouse_x;

  int current_mouse_y;

  // constructor
  public MainCanvas(FrEnd mother) {
    this.parent = mother;

    this.panel.addMouseMotionListener(new MouseMotionListener() {
      public void mouseMoved(MouseEvent e) {
        FrEnd.mouse_pressed = false;

        final MainCanvas canvas = getCanvas();

        canvas.current_mouse_x = e.getX() << Coords.shift;
        canvas.current_mouse_y = e.getY() << Coords.shift;
      }

      public void mouseDragged(MouseEvent e) {
        final MainCanvas canvas = getCanvas();
        canvas.modifiers = e.getModifiers();
        canvas.current_mouse_x = e.getX() << Coords.shift;
        canvas.current_mouse_y = e.getY() << Coords.shift;
        
        FrEnd.new_message_manager.add(new NewMessage(new Point(e.getX() << Coords.shift, e.getY() << Coords.shift)) {
          public Object execute() {
            final Point p = (Point) this.context;
            FrEnd.springieMouseDragged(p.x, p.y);
            return null;
          }
        });
      }
    });

    this.panel.addMouseListener(new MouseListener() {
      public void mouseReleased(MouseEvent e) {
        FrEnd.mouse_pressed = false;
        FrEnd.new_message_manager.add(new NewMessage(new Point(e.getX() << Coords.shift, e.getY() << Coords.shift)) {
          public Object execute() {
            final Point p = (Point) this.context;
            FrEnd.springieMouseReleased(p.x, p.y);
            return null;
          }
        });
      }

      public void mousePressed(MouseEvent e) {
        final MainCanvas canvas = getCanvas();

        canvas.modifiers = e.getModifiers();
        FrEnd.mouse_pressed = true;

        FrEnd.new_message_manager.add(new NewMessage(new Point(e.getX() << Coords.shift, e.getY() << Coords.shift)) {
          public Object execute() {
            final Point p = (Point) this.context;
            FrEnd.springieMousePressed(p.x, p.y);
            return null;
          }
        });
      }

      public void mouseClicked(MouseEvent e) {
        final RendererInfoButton info_button = getInfoButton();
        info_button.mouseClicked(e);

        final MainCanvas canvas = getCanvas();
        canvas.modifiers = e.getModifiers();

        FrEnd.new_message_manager.add(new NewMessage(new Point(e.getX() << Coords.shift, e.getY() << Coords.shift)) {
          public Object execute() {
            final Point p = (Point) this.context;
            FrEnd.springieMouseClicked(p.x, p.y);
            return null;
          }
        });
      }

      public void mouseEntered(MouseEvent e) {
        FrEnd.new_message_manager.add(new NewMessage(new Point(e.getX() << Coords.shift, e.getY() << Coords.shift)) {
          public Object execute() {
            final Point p = (Point) this.context;
            FrEnd.springieMouseEntered(p.x, p.y);
            return null;
          }
        });
      }

      public void mouseExited(MouseEvent e) {
        FrEnd.new_message_manager.add(new NewMessage(new Point(e.getX() << Coords.shift, e.getY() << Coords.shift)) {
          public Object execute() {
            final Point p = (Point) this.context;
            FrEnd.springieMouseExited(p.x, p.y);
            return null;
          }
        });
      }
    });

    this.panel.addKeyListener(new KeyListener() {
      public void keyTyped(KeyEvent e) {
        char c = e.getKeyChar();
        if (c > 'a') {
          c = (char) (c - 'a' + 'A');
        }

        switch (c) {
          case 'a':
          case 'A':
            FrEnd.message_manager.sendMessage(Message.MSG_SELECT_ALL, 0, 0);

            break;

          case 'z':
          case 'Z':
            FrEnd.message_manager.sendMessage(Message.MSG_DESELECT_ALL, 0, 0);

            break;

          case 'c':
          case 'C':
            FrEnd.message_manager.sendMessage(Message.MSG_SELECT_CLAZZ, 0, 0);
            break;

          case 't':
          case 'T':
            FrEnd.message_manager.sendMessage(Message.MSG_SELECT_TYPE, 0, 0);
            break;

          case 'P':
          case 'p':
            //final Checkbox checkbox_pause = FrEnd.panel_fundamental.checkbox_pause;
            //checkbox_pause.setState(!checkbox_pause.getState());
            FrEnd.new_message_manager.add(FrEnd.system_messages.getPauseMessage());
            break;

          default:
            break;
        }
      }

      public void keyPressed(KeyEvent e) {
        //Log.log("Press: " + e.getKeyCode());

        switch (e.getKeyCode()) {
          case KeyEvent.VK_DELETE:
            FrEnd.message_manager.sendMessage(Message.MSG_DELETE, 0, 0);

            break;

          case KeyEvent.VK_SHIFT:
            //shift_pressed = true;

            break;

          case KeyEvent.VK_CONTROL:
            //ctrl_pressed = true;

            break;

          case KeyEvent.VK_ENTER:
            //return_pressed = true;

            break;

          case KeyEvent.VK_ALT:
            //alt_pressed = true;

            break;

          case KeyEvent.VK_ESCAPE:
            break;
          
          default:
            break;

        }
      }

      public void keyReleased(KeyEvent e) {
        //Log.log("Release: " + e.getKeyCode());

        switch (e.getKeyCode()) {
          case KeyEvent.VK_SHIFT:
            //shift_pressed = false;

            break;

          case KeyEvent.VK_CONTROL:
            //ctrl_pressed = false;

            break;

          case KeyEvent.VK_ENTER:
            //return_pressed = false;

            break;

          case KeyEvent.VK_ALT:
            //alt_pressed = false;

            break;
          
          default:
            break;
        }
      }
    });
  }

  // methods
  public final void start_up() {
    if (Coords.x_pixels < 1) {
      Coords.x_pixels = 511;
    }

    if (Coords.y_pixels < 1) {
      Coords.y_pixels = 511;
    }

    this.img_x = Coords.x_pixels;
    this.img_y = Coords.y_pixels;

    this.image_offscreen = createImage(this.img_x, this.img_y);

    setUpGraphicsHandle();

    ContextMananger.getNodeManager().resetNodeGrid(); // due to resize...

    //StarManager.reset(); // stars need resizing...

    RendererDelegator.repaint_all_objects = true;
  }

  public Image createImage(int x, int y) {
    return this.panel.createImage(x, y);
  }

  public final void setUpGraphicsHandle() {
    if (this.image_offscreen != null) {
      this.graphics_handle = this.image_offscreen.getGraphics();
    }
  }

  public final void forceResize() {
    Coords.x_pixels_old = -1;
    RendererDelegator.repaintAll();
  }

  // draw
  public final void update(Graphics g) {
    this.observer = this.panel;

    if (!RendererDelegator.isOldDoubleBuffer()) {
      processAllMessages();
    }

    if (FrEnd.isAnimationInactive()) {
      if (!RendererDelegator.repaint_all_objects) {
        if (!RendererDelegator.repaint_some_objects) {
          return;
        }
      }
    }

    RendererDelegator.repaint_some_objects = false;

    setUpCoordsSize();

    if ((Coords.x_pixels != Coords.x_pixels_old)
      || (Coords.y_pixels != Coords.y_pixels_old)) {
      this.old_gridsize = this.new_gridsize;

      Coords.x_pixels_old = Coords.x_pixels;
      Coords.y_pixels_old = Coords.y_pixels;

      Coords.x_pixelso2 = Coords.x_pixels >> 1;
      Coords.y_pixelso2 = Coords.y_pixels >> 1;

      start_up();
      RendererDelegator.resize(Coords.x_pixels, Coords.y_pixels);
    }

    if (!RendererDelegator.isOldDoubleBuffer()) {
      this.graphics_handle = g;
      RendererDelegator.redrawChanged(g);
    }

    if (RendererDelegator.isOldDoubleBuffer()) {
      if ((((RendererDelegator.generation) & FrEnd.frame_frequency) == 0)
        || FrEnd.isAnimationInactive()) {
        if (this.image_offscreen != null) {
          g.drawImage(this.image_offscreen, this.xoff, this.yoff, this.panel);
        }
      }
    }

    this.info_button.drawInfoButton(g);
  }

  private void processAllMessages() {
    if (FrEnd.message_manager.current_message != 0) {
      FrEnd.message_manager.process();
    }
    FrEnd.new_message_manager.process();
  }

  public void setUpCoordsSize() {
    this.dim = this.panel.getSize();

    Coords.x_pixels = this.dim.width;
    Coords.y_pixels = this.dim.height;
  }

  public final void paint(Graphics g) {
    RendererDelegator.repaintAll();

    if (!RendererDelegator.isOldDoubleBuffer()) {
      RendererDelegator.virgin_applet = 1; // not known why this hack is necessary :-(
    }

    update(g);
  }

  MainCanvas getCanvas() {
    return this;
  }

  public RendererInfoButton getInfoButton() {
    return this.info_button;
  }
}
