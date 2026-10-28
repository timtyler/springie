// This program has been placed into the public domain by its author.

package com.springie.gui.panels.controls;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.links.LinkManager;
import com.springie.metrics.DampingOptimizer;
import com.springie.metrics.WobbleMeter;
import java.awt.Button;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

public class PanelControlsWobble implements WobbleMeter.Listener,
    DampingOptimizer.Listener {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  public Label label_wobble;

  public Button button_optimise_damping;

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
    this.button_optimise_damping = new Button("Optimise damping");
    this.button_optimise_damping.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        final LinkManager link_manager = ContextManager.getLinkManager();
        if (link_manager == null) {
          return;
        }
        DampingOptimizer.setListener(PanelControlsWobble.this);
        if (DampingOptimizer.start(link_manager)) {
          PanelControlsWobble.this.button_optimise_damping.setEnabled(false);
        } else {
          DampingOptimizer.setListener(null);
        }
      }
    });
    this.panel.add(this.button_optimise_damping);
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

  public void optimizationFinished() {
    DampingOptimizer.setListener(null);
    this.button_optimise_damping.setEnabled(true);
    FrEnd.reflectStatusInGUI();
  }

  public void wobbleUpdated(final int wobble_tenths_percent,
      final boolean selected_only) {
    final long now = System.currentTimeMillis();
    if (now >= this.last_update_millis
        && now - this.last_update_millis < 1000) {
      return;
    }
    this.last_update_millis = now;
    if (wobble_tenths_percent < 0) {
      this.label_wobble.setText("Wobble: --");
    } else {
      final String scope = selected_only ? " (selected links)" : "";
      final String text = "Wobble" + scope + ": "
          + (wobble_tenths_percent / 10) + "."
          + (wobble_tenths_percent % 10) + "%";
      this.label_wobble.setText(text);
    }
  }
}
