package com.springie.render;

import java.awt.Color;
import java.awt.Graphics;

import com.springie.FrEnd;
import com.springie.composite.Reproduction;
import com.springie.context.ContextMananger;
import com.springie.explosions.fragments.LineFragmentManager;
import com.springie.explosions.particles.ParticleManager;
import com.springie.gui.gestures.DragBoxManager;
import com.springie.gui.panels.UpdateEnabledComponents;
import com.springie.preferences.Preferences;
import com.springie.render.modules.ModularRendererBase;
import com.springie.render.modules.modern.ModularRendererNew;
import com.springie.utilities.random.JUR;
import com.springie.world.WorldManager;

/*
 * To do: ======
 * 
 * Add a "low number of bits" live/dead array - to postpone cache thrashing...?
 * 
 */

public final class RendererDelegator {
  private RendererDelegator() {
    // ...
  }

  public static ModularRendererBase renderer = new ModularRendererNew();

  public static int virgin_applet;

  static int direction = 1;

  public static int generation;

  public static boolean repaint_all_objects = true;

  public static boolean repaint_some_objects = true;

  public static Graphics graphics_handle;

  static JUR rnd = new JUR();

  public static int color_background_number = 0xFF000000;

  public static Color color_background = new Color(
      color_background_number);

  public static int colour_selected_number = 0xFFFF0000;

  public static Color colour_selected = new Color(colour_selected_number);

  public static int color_charge_number = 0xFF0000C0;

  public static boolean fat_struts = true;

  static long time_last_ms;

  static int frame_count;

  public static void repaintAll() {
    if (renderer instanceof ModularRendererNew) {
      repaint_some_objects = true;
    } else {
      repaint_all_objects = true;
    }
  }


  public static boolean isOldDoubleBuffer() {
    if (renderer instanceof ModularRendererNew) {
      return false;
    }
    return isUnderlyingOldDoubleBuffer();
  }

  public static boolean isUnderlyingOldDoubleBuffer() {
    final Object o = FrEnd.preferences.map
        .get(Preferences.renderer_old_double_buffer);

    return o.equals(Boolean.TRUE);
  }

  public static boolean isNewDoubleBuffer() {
    final Object o = FrEnd.preferences.map
        .get(Preferences.renderer_new_double_buffer);

    return o.equals(Boolean.TRUE);
  }

  //static void resetGrid() {
    //RendererDelegator.repaint_all_objects = true;
  //}

  private static void incrementGenerationCount() {
    if (!FrEnd.paused) {
      RendererDelegator.generation += RendererDelegator.direction;
    }
  }

  public static void setColour(int colour) {
    RendererDelegator.graphics_handle.setColor(new Color(colour));
    if (FrEnd.xor) {
      RendererDelegator.graphics_handle.setXORMode(color_background);
    }
  }

  public static void redrawChanged(Graphics graphics) {
    RendererDelegator.graphics_handle = graphics;

    possibleInitialClear(graphics);

    RendererDelegator.passOnToUpdateMethods(graphics);

    renderDragBox(graphics);

    updateFramePerSecondCounter();
    
    UpdateEnabledComponents.actuallyUpdate();
  }

  private static void updateFramePerSecondCounter() {
    frame_count++;
    if (frame_count == 50) {
      long time_this_ms = System.currentTimeMillis();
      final long duration = time_this_ms - time_last_ms;
      final double fps = frame_count / (duration / 1000.0D);
      time_last_ms = time_this_ms;
      frame_count = 0;
      String fps_str = "" + fps;
      if (fps_str.length() > 5) {
        fps_str = fps_str.substring(0, 5);
      }
      FrEnd.panel_statistics.label_fps_value.setText(fps_str);
    }
  }

  private static void possibleInitialClear(Graphics graphics) {
    if (FrEnd.module) {
      FrEnd.extension.update();
    }

    if (RendererDelegator.repaint_all_objects) {
      if (RendererDelegator.virgin_applet > 0) {
        RendererDelegator.virgin_applet--;
      } else {
        // first ensure there is no clip rectangle!
        graphics.setClip(0, 0, 19999, 19999);
        // then clear the big rectangle
        graphics.setColor(color_background);
        graphics.fillRect(0, 0, 19999, 19999);
        renderer.reset();
        RendererDelegator.repaint_all_objects = false;
      }
    }

    if (RendererDelegator.repaint_all_objects) {
      ContextMananger.getNodeManager().renderer.clear();
      ContextMananger.getNodeManager().renderer2.clear();
    }
  }

  private static void renderDragBox(Graphics graphics) {
    final DragBoxManager drag_box_manager = FrEnd.perform_actions.drag_box_manager;
    final boolean repaint = drag_box_manager.drag_box_end != null;

    final RendererDragBox drag_box_renderer = ContextMananger.getNodeManager().renderer.renderer_drag_box;
    drag_box_renderer.draw(graphics, FrEnd.perform_actions.drag_box_manager);

    if (repaint) {
      FrEnd.main_canvas.panel.repaint();
      repaint_some_objects = true;
    }
  }

  static void passOnToUpdateMethods(Graphics graphics) {
    RendererDelegator.incrementGenerationCount();

    if (!FrEnd.paused) {
      ParticleManager.update();
      LineFragmentManager.update();
      Reproduction.handleReproduction(ContextMananger.getNodeManager().creature_manager);
    }

    ContextMananger.getNodeManager().nodeAndLinkUpdate();

    renderer.repaint(graphics, ContextMananger.getNodeManager());

    WorldManager.privateWorldUnbufferedUpdate();
  }

  public static void callUpdateMethods() {
    if (RendererDelegator.isOldDoubleBuffer()) {
      RendererDelegator.graphics_handle = FrEnd.main_canvas.graphics_handle;

      ParticleManager.update();
      LineFragmentManager.update();
      Reproduction.handleReproduction(ContextMananger.getNodeManager().creature_manager);

      RendererDelegator.incrementGenerationCount();
    }
  }

  public static void colourZero() {
    RendererDelegator.setColour(color_background_number);
  }

  public static void resize(int x_pixels, int y_pixels) {
    renderer.resize(x_pixels, y_pixels);
  }
}
