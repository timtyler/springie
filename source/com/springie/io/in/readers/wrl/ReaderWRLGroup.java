// * Read in information from files...

package com.springie.io.in.readers.wrl;

import java.util.ArrayList;

import com.springie.render.modules.modern.Double3D;

class ReaderWRLGroup {
  int colour;
  ArrayList<Double3D> points = new ArrayList<>();
  ArrayList<ArrayList<Integer>> faces = new ArrayList<>();
}
