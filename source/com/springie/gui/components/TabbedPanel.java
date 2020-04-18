// This code has been placed into the public domain by its author

package com.springie.gui.components;

import java.applet.Applet;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Vector;

import com.tifsoft.Forget;

/**
 * **************************************************************************
 * TabPanel is a container for a set of tabbed cards, lying atop each other, but
 * with the labelled tabs exposed at the top. That is, the classic Tab Folder.
 * Each card is an awt.component of whatever design you wish. The topmost card
 * can be selected programmatically (Using first(), last(), next(), previous(),
 * or show(name)), or by clicking on the tab with the mouse.
 * <P>
 * Components should be added using add(name,component)); the name is used to
 * label the tab. If you set the layout manager, it should be a subclass of
 * CardLayout. You probably want to setBackground() to a color contrasting that
 * of the parent and the components.
 * <P>
 * Whenever a card is selected (whether by software or mouse), an event with id =
 * Event.WINDOW_EXPOSE is sent to the selected component. Handling this event
 * may be useful for deferred initialization.
 */
public class TabbedPanel extends Panel implements MouseListener,
    MouseMotionListener {
  /**
   * 
   */
  static final long serialVersionUID = 6300547944769698100L;

  /** The width of the margins around the cards. */
  // width of margins around cards
  public int margin = 3;

  public int margin_top = 2;

  // for tab labels
  Font tabFont;

  FontMetrics metric;

  // total # of cards
  int nCards;

  // contains the (interned) card names
  Vector names = new Vector(10, 10);

  // position & width of each tab
  int[] pos;

  int[] width;

  // index of selected (displayed) card
  int selected;

  // left shift to allow for `too many' tabs
  int offset;

  // height of tab (set from tabFont)
  int tabH;

  // #points along edges: must = (2*int + 2)
  int tabN = 12;

  Color colour_inactive_foreground = new Color(0x202020);

  Color colour_inactive_tab_background = new Color(0xA8A8A8);

  // also coordinates of tab edge curves
  int[][] tabLeft = new int[2][this.tabN];

  int[][] tabRight = new int[2][this.tabN];

  Image offscreen;

  Applet applet;

  /** Creates an empty TabPanel. */
  public TabbedPanel() {
    setLayout(new CardLayout());
    setTabFont(new Font("Helvetica", Font.BOLD, 12));

    // should add various listeners...
    addMouseListener(this);
  }

  /*****************************************************************************
   * internals
   */

  // find index of a given component
  int findComponent(Component c) {
    for (int i = 0; i < this.nCards; i++) {
      if (getComponent(i) == c) {
        return i;
      }
    }
    return -1;
  }

  /*****************************************************************************
   * Adding & Removing components
   */

  /** Add a card, component, to the TabPanel with a given name. */
  public Component add(String name, Component component) {
    final String name_interned = name.intern();
    // Let layout manager do its job
    super.add(name_interned, component);
    // if name isn't already present
    if (!this.names.contains(name_interned)) {
      this.names.addElement(name_interned);
      this.nCards++;
      if (isShowing()) {
        computeTabs();
        repaint();
      }
    }
    return component;
  }

  /** remove the card, component, from the TabPanel. */
  public void remove(Component component) {
    final int i = findComponent(component);
    // Let layout manager do its job
    super.remove(component);
    // but we'll record our part.
    this.names.removeElementAt(i);
    this.nCards--;
    if (i < this.selected) {
      setSelected(this.selected - 1, true);
      // was selected, select another
    } else if ((i == this.selected) && (this.nCards > 0)) {
      setSelected(this.selected % this.nCards, true);
    }
    // already showing? better rebuild!
    if (isShowing()) {
      computeTabs();
      repaint();
    }
  }

  /** remove the card having the given name from the TabPanel. */
  public void remove(String name) {
    final int i = this.names.indexOf(name.intern());
    if (i != -1) {
      remove(getComponent(i));
    }
  }

  /** remove all cards from the TabPanel. */
  public void removeAll() {
    super.removeAll();
    this.names.removeAllElements();
    repaint();
  }

  /*****************************************************************************
   * Component Selection
   */

  void setSelected(int i, boolean force) {
    if (force || ((i != this.selected) && (i >= 0) && (i < this.nCards))) {
      if (this.nCards > 0) {
        this.selected = i % this.nCards;
      }
      ((CardLayout) getLayout()).show(this, (String) this.names.elementAt(i));
      repaint();
      final Component c = getComponent(i);
      // ?
      // c.postEvent(new Event(c, Event.WINDOW_EXPOSE, this));
      c.validate(); // is this enough?
    }
  }

  /** Select the first card in the Panel. */
  public void first() {
    setSelected(0, false);
  }

  /** Select the last card in the Panel. */
  public void last() {
    setSelected(this.nCards - 1, false);
  }

  /** Select the next card in the Panel. */
  public void next() {
    setSelected((this.selected + 1) % this.nCards, false);
  }

  /** Select the previous card in the Panel. */
  public void previous() {
    setSelected((this.selected - 1 + this.nCards) % this.nCards, false);
  }

  /** Select the named card in the Panel. */
  public void show(String name) {
    setSelected(this.names.indexOf(name.intern()), false);
  }

  /** Select the card component in the Panel. */
  public void show(Component component) {
    setSelected(findComponent(component), false);
  }

  int cardAt(int x, int y) {
    // inside tab section?
    int xx = x;
    if (y <= this.tabH) {
      xx += this.offset;
      for (int i = 0; i < this.nCards; i++) {
        if ((this.pos[i] <= xx) && (xx < this.pos[i + 1])) {
          return i;
        }
      }
    }

    return -1;
  }

  /**
   * Return a mouse documentation string for selecting this card. (ie. the
   * applet status line (if there is an applet), for when the mouse is over the
   * tab). This may be overridden by a subclass, if desired. The default is to
   * use the "Select tab card " + name.
   */
  public String documentCard(String name) {
    return "Select tab card " + name;
  }

  /*****************************************************************************
   * Methods involving size and layout
   */

  /** Allocates extra margins to give the cards some `body'. */
  public Insets insets() {
    return new Insets(this.margin_top + this.tabH + this.margin, this.margin,
        this.margin, this.margin);
  }

  /**
   * Specify the Font to be used for labeling the Tabs. This avoids getting in
   * the way of cards inheriting default fonts from the TabPanel's container.
   */
  public void setTabFont(Font font) {
    this.tabFont = font;
    this.metric = getFontMetrics(font);
    final int r = (this.metric.getHeight() + 1) / 2;
    this.tabH = 2 * r;
    // Compute boundaries for the tab edges.
    final int nn = (this.tabN - 2) / 2;
    int c = nn;
    int s = c;
    double a;
    for (int i = 0; i <= nn; i++) {
      a = Math.PI * i / 2 / nn;
      c = (int) (r * Math.cos(a));
      s = (int) (r * Math.sin(a));
      this.tabLeft[0][i] = s;
      this.tabLeft[1][i] = r + c;
      this.tabLeft[0][i + nn] = this.tabH - c;
      this.tabLeft[1][i + nn] = r - s;
    }
    this.tabLeft[0][2 * nn + 1] = this.tabH;
    this.tabLeft[1][2 * nn + 1] = this.margin_top + this.tabH;
    for (int i = 0; i < this.tabN; i++) {
      this.tabRight[0][i] = -this.tabLeft[0][i];
      this.tabRight[1][i] = this.tabLeft[1][i];
    }
  }

  // Compute positions of the tabs.
  void computeTabs() {
    if ((this.pos == null) || (this.pos.length <= this.nCards)) {
      this.width = new int[this.nCards + 1];
      this.pos = new int[this.nCards + 1];
    } // make sure pos & width are big enough.
    int x = this.tabH / 2;
    // size the tabs & reshape the panes.
    for (int i = 0; i < this.nCards; i++) {
      this.pos[i] = x;
      this.width[i] = this.tabH
          + this.metric.stringWidth((String) this.names.elementAt(i));
      x += this.width[i];
    }
    this.pos[this.nCards] = x;
    final Dimension dim = getSize();
    int w = dim.width;
    if ((this.offscreen == null)
        || (this.offscreen.getHeight(this) < this.margin_top + this.tabH)
        || (this.offscreen.getWidth(this) < w)) {
      if (w < 1) {
        w = 1;
      }
      final int h = this.margin_top + this.tabH;
      //Log.log("" + w);
      //Log.log("" + h);
      this.offscreen = createImage(w, h);
    }
  }

  /** Computes tab geometry while laying out the panels components. */
  public void doLayout() {
    super.doLayout();
    // make sure tabs are computed on resize
    computeTabs();
  }

  /*****************************************************************************
   * Painting the tabs
   * 
   * @param selected2
   */

  void paintTabEdge(Graphics g, int x, int[][] edges, boolean selected) {
    g.translate(x, this.margin_top);
    final Color bg = selected ? getBackground() : this.colour_inactive_tab_background;
    g.setColor(bg);
    g.fillPolygon(edges[0], edges[1], this.tabN);
    g.setColor(getForeground());
    for (int i = 0; i < this.tabN - 2; i++) {
      g.drawLine(edges[0][i], edges[1][i], edges[0][i + 1], edges[1][i + 1]);
    }
    g.translate(-x, -this.margin_top);
  }

  void paintTab(Graphics g, int x, int p, boolean selected) {
    final int r = this.tabH / 2;
    final int w = this.width[p];
    paintTabEdge(g, x - r, this.tabLeft, selected);
    paintTabEdge(g, x + w + r, this.tabRight, selected);
    // for some reason, this draws in the wrong place on window `repair'!!!
    // g.clearRect(x+r,0,w-tabH,tabH);

    final Color bg = selected ? getBackground() : this.colour_inactive_tab_background;
    g.setColor(bg);
    g.fillRect(x + r, this.margin_top, w - this.tabH, this.margin_top
        + this.tabH);
    g.setColor(getForeground());
    g.drawLine(x + r, this.margin_top, x + w - r, this.margin_top);
    g.setFont(this.tabFont);
    if (!selected) {
      g.setColor(this.colour_inactive_foreground);
    }
    g.drawString((String) this.names.elementAt(p), x + r, this.margin_top
        + this.tabH - this.metric.getDescent());
  }

  /**
   * Update (repaint) the TabPanel. Since paint handles the background, we just
   * call paint directly.
   */
  public void update(Graphics g) {
    paint(g);
  }

  /** Paint the tabs in a row atop the cards. */
  public void paint(Graphics gg) {
    // Dimension sz = size();
    final Graphics g = this.offscreen.getGraphics();
    final Dimension dim = getSize();
    int x = dim.width - 1;
    int w = x;
    final int h = dim.height - 1;
    final int r = this.tabH / 2;
    int j = this.selected;
    final int s = j;
    // These come into play when the tabs span more than the panel width.
    // Show some `shadow' tabs at the ends to represent those not displayed.
    // how wide of a shadow (pixels) to show
    final int shadow = 4;

    final int nShadows = 3;
    // final Color backback = getParent().getBackground();

    // Fill in tab area in the PARENT's color
    g.setColor(getParent().getBackground());
    g.fillRect(0, 0, w + 1, this.tabH);
    g.setColor(getForeground());
    if (this.nCards == 0) {
      g.drawLine(0, this.tabH, w, this.tabH);
    } else {
      // Possibly adjust the offset, so at least the selected tab is visible
      final int offmax = this.pos[s] - r - Math.min(nShadows, s) * shadow;
      final int offmin = this.pos[s + 1] - w + r
          + Math.min(this.nCards - s, nShadows) * shadow;
      if ((this.offset < offmin) || (this.offset > offmax)) {
        this.offset = Math.min(Math.max(0, (offmin + offmax) / 2),
            this.pos[this.nCards] + r - w);
      }

      // Draw first tabs from the left (offscreen ones only partly visible)
      for (j = 0, x = this.offset + r; (j < s) && (this.pos[j] <= x); j++) {
        continue;
      }
      if (j > 0) {
        x = 0;
        for (int i = Math.max(0, j - nShadows); i < j - 1; i++, x += shadow) {
          paintTabEdge(g, x, this.tabLeft, false);
        }
        paintTab(g, x + r, j - 1, false);
      }
      for (int i = j; i < s; i++) {
        paintTab(g, this.pos[i] - this.offset, i, false);
      }

      // Draw last tabs from the right (offscreen ones only partly visible)
      for (j = this.nCards - 1, x = this.offset + w - r; (j > s)
          && (this.pos[j + 1] >= x); j--) {
        continue;
      }
      if (j < this.nCards - 1) {
        x = w;
        for (int i = Math.min(this.nCards - 1, j + nShadows); i > j + 1; i--, x -= shadow) {
          paintTabEdge(g, x, this.tabRight, false);
        }
        paintTab(g, x - r - this.width[j + 1], j + 1, false);
      }
      for (int i = j; i > s; i--) {
        paintTab(g, this.pos[i] - this.offset, i, false);
      }

      // now draw the selected tab on top of the others.
      g.clearRect(this.pos[s] - r - this.offset + 2, this.margin_top
          + this.tabH - 1, this.width[s] + this.tabH - 1, 1);
      paintTab(g, this.pos[s] - this.offset, s, true);
      g.setColor(getForeground());
      // and fixup the baseline so the selected is on `top'.
      g.drawLine(0, this.margin_top + this.tabH - 1, this.pos[s] - r
          - this.offset + 1, this.margin_top + this.tabH - 1);
      g.drawLine(this.pos[s + 1] + r - this.offset - 1, this.margin_top
          + this.tabH - 1, w, this.margin_top + this.tabH - 1);
    }
    gg.drawImage(this.offscreen, 0, 0, this);
    gg.drawLine(w, this.margin_top + this.tabH, w, h);
    gg.drawLine(w, h, 0, h);
    gg.drawLine(0, h, 0, this.margin_top + this.tabH);
  }

  public void mouseClicked(MouseEvent e) {
    final int i = cardAt(e.getX(), e.getY());
    if (i != -1) {
      setSelected(i, false);
    }
  }

  public void mousePressed(MouseEvent e) {
    Forget.about(e);
  }

  public void mouseReleased(MouseEvent e) {
    Forget.about(e);
  }

  public void mouseEntered(MouseEvent e) {
    Forget.about(e);
  }

  public void mouseExited(MouseEvent e) {
    Forget.about(e);

    if (this.applet != null) {
      this.applet.showStatus("");
    }
  }

  public void mouseDragged(MouseEvent e) {
    Forget.about(e);
  }

  public void mouseMoved(MouseEvent e) {
    if (this.applet == null) {
      Component c = getParent();
      while (c != null) {
        if (c instanceof Applet) {
          this.applet = (Applet) c;
        }
        c = c.getParent();
      }
    }
    if (this.applet != null) {
      final int i = cardAt(e.getX(), e.getY());
      if (i != -1) {
        this.applet.showStatus(documentCard((String) this.names.elementAt(i)));
      }
    }
  }
}
