// This program has been placed into the public domain by its author.

package com.springie.gui.components;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.ArrayList;

/**
 * A FlowLayout that wraps and, unlike FlowLayout, reports the wrapped
 * height in its preferred size -- so a container whose children wrap onto
 * two rows actually gets two rows of space instead of stranding the second
 * row clipped and unreachable.
 *
 * <p>FlowLayout's preferred height ignores wrapping entirely: it is always
 * the height of a single row. Any parent that sizes from the preferred
 * size (BorderLayout.SOUTH, GridLayout, ...) then keeps the bar one row
 * high while the layout wraps onto a clipped second row -- and the clipped
 * children still report isShowing() == true, so the loss is silent. This
 * layout simulates the wrap at the container's current width and returns
 * that height, so the bar grows to two (or more) rows when the window
 * narrows and everything stays reachable.
 *
 * <p>Nested WrapLayout containers are handled too: when a child using this
 * layout is squeezed narrower than its natural width, its height is
 * recomputed for the width it actually gets, so e.g. a button bar wraps
 * internally instead of clipping.
 *
 * <p>One wrinkle: when the window is resized, the parent asks for the
 * preferred height before the new width has been assigned, so the height
 * can lag one layout pass behind. After laying out, this manager compares
 * the height the wrap actually needs at the real width with the height it
 * was given and, if they differ, schedules a single revalidate on the
 * event queue so the size converges. The second pass always agrees, so
 * this cannot loop.
 */
public class WrapLayout extends FlowLayout {

  static final long serialVersionUID = 1L;

  /**
   * The (width, wanted height) of the last scheduled convergence
   * revalidate, so a parent that never honours the preferred height does
   * not get an endless stream of them.
   */
  private int revalidate_width = -1;

  private int revalidate_wanted = -1;

  public WrapLayout() {
    super();
  }

  public WrapLayout(int align) {
    super(align);
  }

  public WrapLayout(int align, int hgap, int vgap) {
    super(align, hgap, vgap);
  }

  @Override
  public Dimension preferredLayoutSize(Container target) {
    return wrapSizeAt(target, effectiveWidth(target), true);
  }

  @Override
  public Dimension minimumLayoutSize(Container target) {
    return wrapSizeAt(target, effectiveWidth(target), false);
  }

  @Override
  public void layoutContainer(Container target) {
    synchronized (target.getTreeLock()) {
      final Insets insets = target.getInsets();
      final int max_width = Math.max(0,
          target.getWidth() - insets.left - insets.right);
      final int align = getAlignment();

      final ArrayList<Row> rows = buildRows(target, max_width, true);

      int y = insets.top;
      for (final Row row : rows) {
        int x = insets.left;
        if (align == FlowLayout.CENTER) {
          x += Math.max(0, (max_width - row.width) / 2);
        } else if (align == FlowLayout.RIGHT
            || align == FlowLayout.TRAILING) {
          x += Math.max(0, max_width - row.width);
        }
        for (final Placed placed : row.items) {
          // Top-aligned within the row, like FlowLayout.
          placed.component.setBounds(x, y, placed.width, placed.height);
          x += placed.width + getHgap();
        }
        y += row.height + getVgap();
      }

      // The parent sized this container from a preferred height computed
      // at the old width; if the wrap at the real width needs a different
      // height, ask for one more layout pass so the size converges. The
      // (width, wanted) guard keeps this to one revalidate per distinct
      // mismatch: a parent that never honours the preferred height must
      // not get an endless stream of revalidates.
      if (target.getWidth() > 0) {
        final int wanted = wrapSizeAt(target, target.getWidth(), true).height;
        if (wanted != target.getHeight()
            && (target.getWidth() != this.revalidate_width
                || wanted != this.revalidate_wanted)) {
          this.revalidate_width = target.getWidth();
          this.revalidate_wanted = wanted;
          EventQueue.invokeLater(() -> {
            target.invalidate();
            Container top = target;
            while (top.getParent() != null) {
              top = top.getParent();
            }
            top.validate();
          });
        }
      }
    }
  }

  /**
   * Groups the visible children into rows at the given width. Each placed
   * child carries the width/height it was assigned; nested WrapLayout
   * children squeezed narrower than their natural width are assigned the
   * available width and a height recomputed for it.
   */
  private ArrayList<Row> buildRows(Container target, int max_width,
      boolean preferred) {
    final ArrayList<Row> rows = new ArrayList<>();
    Row row = new Row();
    final int count = target.getComponentCount();
    for (int i = 0; i < count; i++) {
      final Component c = target.getComponent(i);
      if (!c.isVisible()) {
        continue;
      }
      // Wrap on the child's natural width: a nested wrapping container
      // that doesn't fit moves to its own row rather than being squeezed
      // into whatever space is left.
      final int natural = naturalWidth(c, preferred);
      if (!row.items.isEmpty()
          && row.width + getHgap() + natural > max_width) {
        rows.add(row);
        row = new Row();
      }
      int available = max_width - row.width
          - (row.items.isEmpty() ? 0 : getHgap());
      if (available <= 0 && !row.items.isEmpty()) {
        // A previous child overflowed the row; don't wedge this one into
        // zero or negative space.
        rows.add(row);
        row = new Row();
        available = max_width;
      }
      final int[] wh = assignedSize(c, available, preferred);
      if (!row.items.isEmpty()) {
        row.width += getHgap();
      }
      row.items.add(new Placed(c, wh[0], wh[1]));
      row.width += wh[0];
      row.height = Math.max(row.height, wh[1]);
    }
    if (!row.items.isEmpty()) {
      rows.add(row);
    }
    return rows;
  }

  /**
   * The width the child would take on a row of its own: its preferred
   * (or minimum) width, or the natural single-row width for a nested
   * wrapping container.
   */
  private int naturalWidth(Component c, boolean preferred) {
    final WrapLayout nested = nestedLayout(c);
    if (nested != null) {
      return nested.wrapSizeAt((Container) c, Integer.MAX_VALUE,
          preferred).width;
    }
    return (preferred ? c.getPreferredSize() : c.getMinimumSize()).width;
  }

  /**
   * The width/height to give a child with the given available row width.
   * A nested WrapLayout child is measured at the width it will actually
   * get (capped at its natural width); every other child keeps its
   * preferred/minimum size, as with FlowLayout.
   */
  private int[] assignedSize(Component c, int available, boolean preferred) {
    final WrapLayout nested = nestedLayout(c);
    if (nested == null) {
      final Dimension d = preferred ? c.getPreferredSize()
          : c.getMinimumSize();
      return new int[] { d.width, d.height };
    }
    final Container child = (Container) c;
    final int w = Math.min(naturalWidth(c, preferred), Math.max(0, available));
    final int h = nested.wrapSizeAt(child, Math.max(1, w), preferred).height;
    return new int[] { w, h };
  }

  private static WrapLayout nestedLayout(Component c) {
    if (c instanceof Container) {
      final java.awt.LayoutManager layout = ((Container) c).getLayout();
      if (layout instanceof WrapLayout) {
        return (WrapLayout) layout;
      }
    }
    return null;
  }

  /**
   * Simulates the wrap at the given width and returns the size the
   * container needs: the wrapped height, and the width it was asked about
   * (or the natural single-row width when unbounded).
   */
  private Dimension wrapSizeAt(Container target, int width,
      boolean preferred) {
    synchronized (target.getTreeLock()) {
      final Insets insets = target.getInsets();
      final int max_width = width == Integer.MAX_VALUE ? width
          : Math.max(0, width - insets.left - insets.right);
      final ArrayList<Row> rows = buildRows(target, max_width, preferred);
      int height = insets.top + insets.bottom;
      int content_width = insets.left + insets.right;
      for (final Row row : rows) {
        height += row.height + getVgap();
        content_width = Math.max(content_width,
            row.width + insets.left + insets.right);
      }
      if (!rows.isEmpty()) {
        height -= getVgap();
      }
      // The width this container actually needs: its widest row, capped
      // at the width it was asked about. (Returning the asked width
      // itself would make the preferred width "sticky": once measured
      // wide, always wide, and parents would keep handing back the stale
      // width instead of the narrower one the content fits in.)
      final int result_width = width == Integer.MAX_VALUE ? content_width
          : Math.min(content_width, width);
      return new Dimension(result_width, height);
    }
  }

  /**
   * The width to simulate the wrap at: the container's own width if it
   * has been laid out, otherwise the nearest ancestor with a width
   * (during a resize the ancestors already have the new width while this
   * container is still at the old one), otherwise unbounded.
   */
  private static int effectiveWidth(Container target) {
    int w = target.getWidth();
    if (w > 0) {
      return w;
    }
    for (Container p = target.getParent(); p != null; p = p.getParent()) {
      w = p.getWidth();
      if (w > 0) {
        return w;
      }
    }
    return Integer.MAX_VALUE;
  }

  /** One row of placed children. */
  private static final class Row {
    final ArrayList<Placed> items = new ArrayList<>();
    int width;
    int height;
  }

  /** A child with the width/height it was assigned in its row. */
  private static final class Placed {
    final Component component;
    final int width;
    final int height;

    Placed(Component component, int width, int height) {
      this.component = component;
      this.width = width;
      this.height = height;
    }
  }
}
