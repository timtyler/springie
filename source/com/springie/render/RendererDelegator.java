package com.springie.render;

import java.awt.Color;
import java.awt.Graphics;

import com.springie.FrEnd;
import com.springie.composite.Reproduction;
import com.springie.context.ContextManager;
import com.springie.explosions.fragments.LineFragmentManager;
import com.springie.explosions.particles.ParticleManager;
import com.springie.gui.gestures.DragBoxManager;
import com.springie.gui.panels.UpdateEnabledComponents;
import com.springie.preferences.Preferences;
import com.springie.render.modules.ModularRendererBase;
import com.springie.render.modules.modern.ModularRendererNew;
import com.springie.render.modules.raytraced.ModularRendererRaytraced;
import com.springie.render.modules.modern.RendererBinManager;
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

  public static Color color_background = new Color(color_background_number);

  public static int colour_selected_number = 0xFFFF0000;

  public static Color colour_selected = new Color(colour_selected_number);

  public static int color_charge_number = 0xFF0000C0;

  /**
   * Number of sides of the open tube that struts and cables render as
   * (2, 3, 4, 6 or 8). 2 is a cylindrical billboard that always faces
   * the viewer; 3 is the fastest true tube, 8 the smoothest. The tube
   * ends are left open: capping them costs polygons without helping
   * the picture.
   */
  public static int link_sides = 2;

  static long time_last_ms;

  static int frame_count;

  public static void repaintAll() {
    if (renderer instanceof ModularRendererNew
        || renderer instanceof ModularRendererRaytraced) {
      repaint_some_objects = true;
    } else {
      repaint_all_objects = true;
    }
  }


  public static boolean isOldDoubleBuffer() {
    if (renderer instanceof ModularRendererNew
        || renderer instanceof ModularRendererRaytraced) {
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
      ContextManager.getNodeManager().renderer.clear();
      ContextManager.getNodeManager().renderer2.clear();
    }
  }

  private static void renderDragBox(Graphics graphics) {
    final DragBoxManager drag_box_manager = FrEnd.perform_actions.drag_box_manager;
    final boolean repaint = drag_box_manager.drag_box_end != null;

    // The drag box erases its old rectangle with the background colour after
    // the bins have rendered, damaging their pixels; the next frame must
    // re-render every bin to repair it.
    RendererBinManager.drag_box_damaged_last_frame = repaint;

    final RendererDragBox drag_box_renderer = ContextManager.getNodeManager().renderer.renderer_drag_box;
    drag_box_renderer.draw(graphics, FrEnd.perform_actions.drag_box_manager);

    if (repaint) {
      FrEnd.main_canvas.panel.repaint();
      repaint_some_objects = true;
    }
  }

  static void passOnToUpdateMethods(Graphics graphics) {
    // A slow renderer (the ray-traced one) holds the model while its
    // frame renders, so every model state is rendered exactly once, in
    // order, and the animation runs at render speed rather than skipping
    // states to stay real-time.
    final boolean hold = renderer.holdModelForFrame();

    if (!hold) {
      RendererDelegator.incrementGenerationCount();

      if (!FrEnd.paused) {
        ParticleManager.update();
        LineFragmentManager.update();
        Reproduction.handleReproduction(ContextManager.getNodeManager().creature_manager);
      }

      ContextManager.getNodeManager().nodeAndLinkUpdate();
    }

    renderer.repaint(graphics, ContextManager.getNodeManager());

    if (!hold) {
      WorldManager.privateWorldUnbufferedUpdate();
    }
  }

  public static void callUpdateMethods() {
    if (RendererDelegator.isOldDoubleBuffer()
        && !renderer.holdModelForFrame()) {
      RendererDelegator.graphics_handle = FrEnd.main_canvas.graphics_handle;

      ParticleManager.update();
      LineFragmentManager.update();
      Reproduction.handleReproduction(ContextManager.getNodeManager().creature_manager);

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
