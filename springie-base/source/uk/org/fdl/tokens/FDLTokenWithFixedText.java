package uk.org.fdl.tokens;

public class FDLTokenWithFixedText extends FDLToken {
  String text = "";
  public FDLTokenWithFixedText(String name, String text) {
    this.name = name;
    this.text = text;
  }
}
