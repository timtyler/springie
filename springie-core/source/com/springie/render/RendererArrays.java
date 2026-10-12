// This code has been placed into the public domain by its author

package com.springie.render;

public class RendererArrays {
  public ArrayOfNodesToRender renderer_node = new ArrayOfNodesToRender();
  public ArrayOfLinksToRender renderer_link = new ArrayOfLinksToRender();
  public ArrayOfFacesToRender renderer_face = new ArrayOfFacesToRender();
  public RendererDragBox renderer_drag_box = new RendererDragBox();

  public int mask = 0xFF00FFFF;
  
  public void setMask(int mask) {
    this.mask = mask;
  }
  
  public void clear() {
    this.renderer_node.clear();
    this.renderer_link.clear();
    this.renderer_face.clear();
  }
}
