package uk.org.fdl.object;

public class FDLObjectBraceList extends FDLObjectCollection {
  public boolean newlines = true;

  public String open = "{";

  public String close = "}";

  static boolean recent_end_tag;
  
  public FDLObjectBraceList() {
    this.separator = " ";
  }

//  public String makeString() {
//    return makeString(0);
//  }
//
//  public String makeString(int indent) {
//    final StringBuffer sb = new StringBuffer();
//
//    outputStartTagAndAttributes(indent, sb);
//
//    outputChildren(indent, sb);
//
//    outputEndTag(indent, sb);
//
//    return sb.toString();
//  }
//
//  private void outputStartTagAndAttributes(final int indent,
//      final StringBuffer sb) {
//    Forget.about(indent);
//    sb.append(this.open);
//    if (this.newlines) {
//      sb.append("\n");
//      recent_end_tag = false;
//    }
//  }
//
//  private void outputChildren(int indent, final StringBuffer sb) {
//    if (this.children != null) {
//      final int children_size = this.children.size();
//      for (int i = 0; i < children_size; i++) {
//        final FDLWriterInterface node = (FDLWriterInterface) this.children
//            .elementAt(i);
//        int spaces = 0;
//        if (this.newlines) {
//          spaces = indent + ZFDLWriterIndent.level;
//        } else if (i > 0) {
//          sb.append(this.separator);
//          spaces = 0;
//        }
//        sb.append(node.makeString(spaces));
//        if (this.newlines) {
//          if (!recent_end_tag || (i < (children_size - 1))) {
//            sb.append("\n");
//          }
//        }
//      }
//    }
//    recent_end_tag = false;
//  }
//
//  private void outputEndTag(int indent, final StringBuffer sb) {
//    if (this.newlines) {
//      FDLWriterStringUtilities.indent(sb, indent);
//    }
//    sb.append(this.close);
//    if (this.newlines) {
//      //if (!recent_end_tag) {
//        sb.append("\n");
//      //}
//      recent_end_tag = true;
//    }
//  }
}
