package com.springie.elements.lists;

import java.util.ArrayList;

import com.springie.elements.base.BaseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListOfIntegers extends BaseType {
  private static final Logger logger = LoggerFactory.getLogger(ListOfIntegers.class);

  ArrayList<Integer> list = new ArrayList<>();

  public final int size() {
    return this.list.size();
  }

  public final void add(int i) {
    this.list.add(new Integer(i));
  }

  public int retreive(int i) {
    return this.list.elementAt(i).intValue();
  }

  public void remove(int n) {
    for (int i = this.list.size(); --i >= 0;) {
      final int value = retreive(i);
      if (value == n) {
        logger.debug("Removing index " + n + " : from list position:" + i);
        this.list.remove(i);
        return;
      }
    }
  }
}
