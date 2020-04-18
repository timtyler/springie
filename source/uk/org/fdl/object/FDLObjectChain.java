package uk.org.fdl.object;

public class FDLObjectChain extends FDLObjectCollection {

  public FDLObjectChain(String separator) {
    this.separator = separator;
  }

//  public String makeString() {
//    return makeString(0);
//  }
//
//  public String makeString(int indent) {
//    final StringBuffer sb = new StringBuffer();
//    FDLWriterStringUtilities.repeat(' ', indent);
//    outputChildren(sb, indent);
//
//    return sb.toString();
//  }
//
//  private void outputChildren(final StringBuffer sb, int indent) {
//    if (this.children != null) {
//      final int children_size = this.children.size();
//      for (int i = 0; i < children_size; i++) {
//        final FDLWriterInterface node = (FDLWriterInterface) this.children
//            .elementAt(i);
//        if (i > 0) {
//          sb.append(this.separator);
//        } else {
//          FDLWriterStringUtilities.indent(sb, indent);
//        }
//        sb.append(node.makeString(indent));
//      }
//    }
//  }
}
