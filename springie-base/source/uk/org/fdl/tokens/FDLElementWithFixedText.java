package uk.org.fdl.tokens;

public class FDLElementWithFixedText extends FDLElement {
  // ...
  public FDLElementWithFixedText(FDLTokenWithFixedText type, int line, int column) {
    this.type = type;
    this.line = line;
    this.column = column;
  }

  public String getText() {
    return ((FDLTokenWithFixedText) this.type).text;
  }
}
