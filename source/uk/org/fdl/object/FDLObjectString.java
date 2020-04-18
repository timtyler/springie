package uk.org.fdl.object;


public class FDLObjectString extends FDLObject {
  public String string;
  
  public FDLObjectString(String string) {
    this.string = string;
  }
  
//  public String makeString() {
//    return makeString(0);
//  }
//
//  public String makeString(int indent) {
//    Forget.about(indent);
//    return "\"" + this.string + "\"";
//  }  
}
