// This program has been placed into the public domain by its author.

package com.springie.gui.frames;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import com.springie.FrEnd;
import com.springie.gui.components.GraphicsDirectory;
import com.springie.utilities.ImageLoader;
import com.springie.utilities.ImageWrapper;
import com.tifsoft.Forget;

public class FrameMaker {
  // static final String program_name = FrEnd.application_name;

  public Frame setUpFrameControls() {
    final Frame frame = new Frame();
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        Forget.about(e);
        FrEnd.frame_controls.setVisible(false);
      }
    });

    frame.add("Center", FrEnd.panel_controls_all.panel);
    frame.pack();
    centreOnScreen(frame, 75, 70);
    frame.setSize(320, 600);
    frame.setTitle(FrEnd.application_name + " - Controls");
    
    final ImageWrapper image = ImageLoader.getImage(GraphicsDirectory.directory
        + "icon_controls.png");
    
    frame.setIconImage(image.getImage());
    return frame;
  }

  public Frame setUpFrameAbout() {
    final Frame frame = new Frame();
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        Forget.about(e);
        FrEnd.frame_panel_about.setVisible(false);
      }
    });

    frame.add("Center", FrEnd.panel_about.panel);
    frame.pack();
    frame.setSize(300, 200); // ?
    frame.setTitle(FrEnd.application_name + " - About box");
    return frame;
  }

  public Frame setUpFrameHelp() {
    final Frame frame = new Frame();
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        Forget.about(e);
        FrEnd.frame_panel_help.setVisible(false);
      }
    });

    frame.add("Center", FrEnd.panel_help.panel);
    frame.pack();
    frame.setSize(400, 300);
    frame.setTitle(FrEnd.application_name + " - Help");

    return frame;
  }

  public static void centreOnScreen(Frame frame, int percentage_x,
      int percentage_y) {
    final Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();
    final Dimension r_in = toolkit.getScreenSize();

    final Rectangle r_out = new Rectangle();

    r_out.width = r_in.width * percentage_x / 100;
    r_out.height = r_in.height * percentage_y / 100;
    r_out.x = (r_in.width - r_out.width) / 2;
    r_out.y = (r_in.height - r_out.height) / 2;

    frame.setBounds(r_out);
  }
}
