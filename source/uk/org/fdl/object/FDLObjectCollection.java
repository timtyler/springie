package uk.org.fdl.object;

import java.util.ArrayList;

class FDLObjectCollection extends FDLObject {  
  public ArrayList children;
  public String separator = " ";

  public ArrayList getChildren() {
    return this.children;
  }

  public void setChildren(ArrayList children) {
    this.children = children;
  }

  public void add(ArrayList children) {
    ensureChildrenExist();
    
    final int children_size = this.children.size();
    for (int i = 0; i < children_size; i++) {
      final FDLObject element = (FDLObject) children.get(i);
      add(element);
    }
  }

  public void add(FDLObject child) {
    ensureChildrenExist();

    this.children.add(child);
  }

  private void ensureChildrenExist() {
    if (this.children == null) {
      this.children = new ArrayList<>();
    }
  }

  public void addContentsOf(FDLObjectCollection child) {
    ensureChildrenExist();

    for (int i = 0; i < child.children.size(); i++) {
      final Object tag = child.children.get(i);
      this.children.add(tag);
    }
  }
}