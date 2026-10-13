package uk.org.fdl.tokens;

public class FDLElementWithVariableText extends FDLElement {
  String text = "";
  public FDLElementWithVariableText(FDLTokenWithVariableText type, String text, int line, int column) {
    this.type = type;
    this.text = text;
    this.line = line;
    this.column = column;
  }
 
  public String getText() {
     return this.text;
  }
}
