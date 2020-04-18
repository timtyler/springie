package uk.org.fdl.tokens;

public abstract class FDLElement {
  public FDLToken type;
  public int line;
  public int column;
  public abstract String getText();
}
