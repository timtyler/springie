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
import com.springie.render.modules.modern.RendererTileManager;
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

  public static int virgin_paint;

  static int direction = 1;

  public static int generation;

  public static boolean repaint_all_objects = true;

  public static boolean repaint_some_objects = true;

  /**
   * Set when possibleInitialClear actually cleared the whole screen.
   * Consumed by redrawChanged, which paints the outside-the-viewpoint
   * shading once, after the fresh frame is up -- never per animation
   * frame.
   */
  private static boolean full_clear_done = false;

  public static Graphics graphics_handle;

  static JUR rnd = new JUR();

  public static int color_background_number = 0xFF000000;

  public static Color color_background = new Color(color_background_number);

  /**
   * When true, the background is the procedural grass/sky texture
   * (ScenicBackground) instead of the flat background colour, in both
   * the default and the ray-traced renderer.
   */
  public static boolean scenic_background = false;

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
  public static int link_sides = 4;

  /**
   * Ray-traced glossiness as a percentage (10-100): each lit surface
   * gets a broad, smooth specular sheen whose strength follows the
   * setting -- 50% means every pixel receives half-strength glossy
   * light, never a per-pixel coin flip. The glossiness_enabled
   * checkbox is the off switch; the dropdown only shows while it is
   * on. Defaults to off.
   */
  public static boolean glossiness_enabled = false;

  public static int glossiness = 50;

  /**
   * Ray-traced shadows. Each surface point casts a ray at the light; when
   * something blocks it, the point gets ambient light only.
   */
  public static boolean shadows = false;

  /**
   * Ray-traced specular highlights as a percentage (10-100): the
   * Blinn-Phong sparkle where a surface reflects the light straight at
   * the viewer. The specular_enabled checkbox is the off switch; the
   * dropdown only shows while it is on. Defaults to on at 100%.
   */
  public static boolean specular_enabled = true;

  public static int specular = 100;

  /**
   * Fresnel rim light as a percentage (10-100). Surfaces gain a
   * view-dependent rim that follows Schlick's approximation: nothing
   * head-on, rising to the full percentage of white light at
   * silhouette (grazing) angles -- the look of real reflective
   * surfaces, which mirror more at a glancing view. The
   * fresnel_enabled checkbox is the off switch; the dropdown only
   * shows while it is on. Like the specular highlight it needs direct
   * light, so shadowed points get none. Defaults to off.
   */
  public static boolean fresnel_enabled = false;

  public static int fresnel = 50;

  /**
   * Fill light as a percentage (10-100): a weak second light from the
   * front-right, mirroring the key light's front-left azimuth, so
   * surfaces turned away from the key still model instead of sitting
   * at the flat diffuse floor. Shadow-independent, so it lifts
   * shadowed areas too. The fill_light_enabled checkbox is the off
   * switch; the dropdown only shows while it is on. Defaults to off.
   */
  public static boolean fill_light_enabled = false;

  public static int fill_light = 50;

  /**
   * Anti-aliasing supersampling factor (1 to 5): 1x1 is off, 2x2, 3x3,
   * 4x4 and 5x5 render that many sub-samples per pixel and average them
   * with a box filter. Affects the modern tiled renderer (tiles are
   * rendered at n-times resolution and downsampled on blit) and the
   * ray-traced renderer (n-by-n sub-pixel rays per pixel). 1 is the
   * default.
   */
  public static int antialiasing = 1;

  /**
   * Pixellation factor (1 to 5): 1x1 is off, 2x2, 3x3, 4x4 and 5x5 render
   * one colour per n-by-n screen block and replicate it across the block,
   * producing a blocky, pixellated display -- the opposite of
   * anti-aliasing. Affects the modern tiled renderer (tiles are rendered
   * at 1/n resolution and nearest-neighbour upsampled on blit) and the
   * ray-traced renderer (one ray per n-by-n block). 1 is the default.
   */
  public static int pixellation = 1;

  static long time_last_ms;

  static int frame_count;

  // Last computed readout, waiting for the next paint to publish it.
  static String fps_string;

  static boolean fps_dirty;

  public static void repaintAll() {
    if (renderer instanceof ModularRendererNew
        || renderer instanceof ModularRendererRaytraced) {
      repaint_some_objects = true;
    } else {
      repaint_all_objects = true;
    }
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

    // Mutually exclusive with message execution (see NewMessageManager):
    // the AWT-thread renderer must not access the model while the
    // animation thread is building it.
    synchronized (ContextManager.class) {
      RendererDelegator.passOnToUpdateMethods(graphics);
    }

    if (RendererDelegator.full_clear_done) {
      RendererDelegator.full_clear_done = false;
      // The whole screen was just redrawn: shade the area outside the
      // viewpoint once, here, on the fresh frame -- never per animation
      // frame, so animating the model costs nothing extra.
      ViewportShade.shadeOutsideBox(graphics);
    }

    renderDragBox(graphics);

    refreshFpsLabel();

    UpdateEnabledComponents.actuallyUpdate();
  }

  /**
   * Records one fully rendered frame for the Statistics tab's
   * frames-per-second readout. Each renderer calls this exactly once
   * per completed frame -- never once per AWT paint, which in
   * ray-traced mode fires far more often than the asynchronous
   * renderer completes frames.
   *
   * <p>The readout refreshes every 50 frames, or every 2 seconds when
   * frames are slow (a ray-traced frame can take seconds), whichever
   * comes first. The label itself is updated on the paint path, so
   * this stays headless-safe for unit tests.
   */
  public static synchronized void countRenderedFrame() {
    final long now = System.currentTimeMillis();
    if (frame_count == 0) {
      // Start the window at the first frame, not at class load.
      time_last_ms = now;
    }
    frame_count++;
    final long duration = now - time_last_ms;
    if (frame_count >= 50 || duration >= 2000) {
      final double fps = frame_count / (duration / 1000.0D);
      time_last_ms = now;
      frame_count = 0;
      fps_string = "" + Math.round(fps);
      fps_dirty = true;
    }
  }

  /**
   * Publishes a freshly computed readout to the Display preferences. Called
   * from the paint path, so the AWT label is always touched on a thread
   * that may touch AWT.
   */
  static synchronized void refreshFpsLabel() {
    if (fps_dirty) {
      fps_dirty = false;
      FrEnd.panel_preferences_display.label_fps_value.setText(fps_string);
    }
  }

  private static void possibleInitialClear(Graphics graphics) {
    if (FrEnd.module) {
      FrEnd.extension.update();
    }

    if (RendererDelegator.repaint_all_objects) {
      if (RendererDelegator.virgin_paint > 0) {
        RendererDelegator.virgin_paint--;
      } else {
        // first ensure there is no clip rectangle!
        graphics.setClip(0, 0, 19999, 19999);
        // then clear the big rectangle
        if (RendererDelegator.scenic_background && Coords.x_pixels > 0
            && Coords.y_pixels > 0) {
          graphics.drawImage(ScenicBackground.imageFor(Coords.x_pixels,
              Coords.y_pixels), 0, 0, null);
        } else {
          graphics.setColor(color_background);
          graphics.fillRect(0, 0, 19999, 19999);
        }
        renderer.reset();
        RendererDelegator.repaint_all_objects = false;
        RendererDelegator.full_clear_done = true;
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

    // While a drag box is active every frame is fully repainted, which
    // covers the previous rectangle. (The drag box itself is only ever
    // drawn -- never erased -- so there is nothing to repair.) The modern
    // renderer forces the damaged tiles dirty and the ray tracer includes
    // the damaged region in its dirty rectangles; the old polygon
    // renderer gets a full clear-and-redraw (see below).
    final RendererDragBox drag_box_renderer = ContextManager.getNodeManager().renderer.renderer_drag_box;
    drag_box_renderer.draw(graphics, FrEnd.perform_actions.drag_box_manager);

    if (repaint) {
      // The ray-traced renderer blits a whole frame per paint; asking for
      // another repaint here turns every drag into a tight repaint loop,
      // and each blit erases the box before it is redrawn -- the box
      // flickers. The ray tracer already repaints on mouse moves (via
      // DragBoxManager.drag) and when frames complete, which is enough
      // to keep the box up to date.
      final boolean raytraced = RendererDelegator.renderer
          instanceof com.springie.render.modules.raytraced.ModularRendererRaytraced;
      if (!raytraced) {
        FrEnd.main_canvas.panel.repaint();
        if (RendererDelegator.renderer
            instanceof com.springie.render.modules.original.ModularRendererOld) {
          // The old polygon renderer paints straight onto the screen with
          // no damage repair, so a draw-only drag rectangle would never be
          // erased: a click leaves a small red box behind, and a real drag
          // trails. Every frame while the box is up must be a full
          // clear-and-redraw instead.
          repaint_all_objects = true;
        } else {
          repaint_some_objects = true;
        }
      }
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

    // While the renderer holds the model for an in-flight frame, its
    // repaint() has nothing new to show until the frame stages -- so skip
    // the call instead of redrawing at the animation tick rate. A staged
    // frame always releases the hold first (the worker sets frame_done
    // before frame_staged), so the finished frame is never skipped here.
    if (!hold || renderer.hasStagedFrame()) {
      renderer.repaint(graphics, ContextManager.getNodeManager());
    }

    if (!hold) {
      WorldManager.privateWorldUnbufferedUpdate();
    }
  }

  public static void colourZero() {
    RendererDelegator.setColour(color_background_number);
  }

  public static void resize(int x_pixels, int y_pixels) {
    renderer.resize(x_pixels, y_pixels);
  }
}
