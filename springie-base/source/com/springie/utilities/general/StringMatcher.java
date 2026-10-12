package com.springie.utilities.general;

public abstract class StringMatcher {
  public abstract boolean matches(String a, String b);
  public abstract String combine(String a, String b);
  public abstract String set(String from, String to, String name);
}
