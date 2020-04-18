package uk.org.fdl.object;

public class FDLObjectIdentifier extends FDLObject {
  public String identifier;
  
  public FDLObjectIdentifier(String identifier) {
    this.identifier = identifier;
  }
  
//  public String makeString() {
//    return makeString(0);
//  }
//
//  public String makeString(int indent) {
//    Forget.about(indent);
//    return this.identifier;
//  }  
}
