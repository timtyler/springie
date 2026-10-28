// This program has been placed into the public domain by its author.

package com.springie.gui.panels.controls;

import com.springie.FrEnd;
import com.springie.metrics.WobbleMeter;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

public class PanelControlsWobble implements WobbleMeter.Listener {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  public Label label_wobble;

  private long last_update_millis;

  public PanelControlsWobble() {
    makePanel();
  }

  void makePanel() {
    this.label_wobble = new Label("Wobble: --", Label.CENTER);
    this.panel.add(this.label_wobble);
    final Label label_note = new Label(
        "Average link-length oscillation over the last 100 ticks.",
        Label.CENTER);
    this.panel.add(label_note);
    this.panel.addHierarchyListener(new HierarchyListener() {
      public void hierarchyChanged(final HierarchyEvent e) {
        if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
          final boolean showing = PanelControlsWobble.this.panel.isShowing();
          if (showing) {
            PanelControlsWobble.this.last_update_millis = 0;
          }
          WobbleMeter.setListener(showing ? PanelControlsWobble.this : null);
          WobbleMeter.setActive(showing);
        }
      }
    });
  }

  public void wobbleUpdated(final int wobble_tenths_percent) {
    final long now = System.currentTimeMillis();
    if (now >= this.last_update_millis
        && now - this.last_update_millis < 1000) {
      return;
    }
    this.last_update_millis = now;
    if (wobble_tenths_percent < 0) {
      this.label_wobble.setText("Wobble: --");
    } else {
      final String text = "Wobble: " + (wobble_tenths_percent / 10) + "."
          + (wobble_tenths_percent % 10) + "%";
      this.label_wobble.setText(text);
    }
  }
}
