// This program has been placed into the public domain by its author.

package com.springie.gui.colourpicker;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import com.tifsoft.Forget;
import com.tifsoft.deprecated.OldMethods;

public class ColourPickerBase extends Component implements MouseListener,
    MouseMotionListener {
  static final long serialVersionUID = 1250;

  int width = -1;

  int height = -1;

  int last_w = -1;

  int last_h = -1;

  int margin = 2;

  int range;

  Image main_colour_palette;

  Graphics grf;

  ColourPickerController colour_picker;

  int x;

  public void init() {
    addMouseMotionListener(this);
    addMouseListener(this);
  }

  public void inform(ColourPickerController cp) {
    this.colour_picker = cp;
  }

  void paintHelper() {
    final OldMethods old = new OldMethods();

    this.width = old.getWidth(this);
    this.height = old.getHeight(this);
    this.range = this.width - this.margin * 2;

    if ((this.width != this.last_w) & (this.height != this.last_h)) {
      this.last_w = this.width;
      this.last_h = this.height;
      this.grf = null;
    }

    if (this.grf == null) {
      this.main_colour_palette = createImage(this.width, this.height);
      this.grf = this.main_colour_palette.getGraphics();
    }

    this.grf.setColor(Color.lightGray);
    this.grf.fillRect(0, 0, this.width, this.margin);
    this.grf
        .fillRect(0, this.height - this.margin * 2, this.width, this.margin);
  }

  void paintMarkers(Graphics g, int x) {
    paintMarkers(g, x / 255.0F);
  }

  void paintMarkers(Graphics g, float v) {
    final int size = 8;
    int xv = (int) (this.range * v);

    final int[] polygon_x = new int[3];
    final int[] polygon_y = new int[3];

    polygon_x[0] = xv - size;
    polygon_x[1] = xv + size;
    polygon_x[2] = xv;

    polygon_y[0] = this.margin;
    polygon_y[1] = this.margin;
    polygon_y[2] = this.margin + size;

    drawThisPolygon(g, polygon_x, polygon_y);

    polygon_y[0] = this.height - this.margin;
    polygon_y[1] = this.height - this.margin;
    polygon_y[2] = this.height - this.margin - size;

    drawThisPolygon(g, polygon_x, polygon_y);
  }

  private void drawThisPolygon(Graphics g, final int[] polygon_x,
      final int[] polygon_y) {
    g.setColor(Color.yellow);
    g.fillPolygon(polygon_x, polygon_y, 3);
    g.setColor(Color.blue);
    g.drawPolygon(polygon_x, polygon_y, 3);
  }

  void clickHelper(MouseEvent e) {
    this.x = e.getX() - this.margin;

    if (this.x < 0) {
      this.x = 0;
    }

    if (this.x > this.range) {
      this.x = this.range;
    }
  }

  public void mouseClicked(MouseEvent e) {
    Forget.about(e);

    throw new RuntimeException("mouseClicked bug");
  }

  public void mouseDragged(MouseEvent e) {
    mouseClicked(e);
  }

  // motion...
  public void mouseReleased(MouseEvent e) {
    Forget.about(e);
  }

  public void mousePressed(MouseEvent e) {
    Forget.about(e);
  }

  public void mouseEntered(MouseEvent e) {
    Forget.about(e);
  }

  public void mouseExited(MouseEvent e) {
    Forget.about(e);
  }

  public void mouseMoved(MouseEvent e) {
    Forget.about(e);
  }

  public void paint(Graphics g) {
    update(g);
  }

  protected void paintString(Graphics g, ColourPickerBase base, String string,
      int colour) {
    final Font font = new Font("sansserif", Font.BOLD, 14);
    g.setFont(font);

    final FontMetrics metric = getFontMetrics(font);

    final int w = metric.stringWidth(string);
    final int h = metric.getHeight();

    final int bottom_x = base.width / 2 - w / 2;
    final int bottom_y = base.height / 2 + h / 2 - metric.getDescent();

    g.setColor(new Color(colour));
    g.drawString(string, bottom_x, bottom_y);
  }

  protected void paintString(Graphics g, ColourPickerBase base, String string) {
    final int colour = this.colour_picker.getColour();
    final int contrasting = blackOrWhite(colour);

    paintString(g, base, string, contrasting);
  }

  private int blackOrWhite(int rgb) {
    final int red = (rgb >> 16) & 0xFF;
    final int green = (rgb >> 8) & 0xFF;
    final int blue = rgb & 0xFF;

    final int tot = red + green + blue;

    return (tot > 384) ? 0xFF000000 : -1;
  }
}