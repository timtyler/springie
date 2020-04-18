package uk.org.fdl.writer;

import uk.org.fdl.object.FDLObject;
import uk.org.fdl.object.FDLObjectBraceList;
import uk.org.fdl.object.FDLObjectBracketList;
import uk.org.fdl.object.FDLObjectChain;
import uk.org.fdl.object.FDLObjectIdentifier;
import uk.org.fdl.object.FDLObjectNumber;
import uk.org.fdl.object.FDLObjectString;

import com.tifsoft.Forget;

public final class FDLWriter {

  static boolean recent_end_tag;
  public static int level = 1;
  
  private FDLWriter() {
    //...
  }
  
  public static String toString(FDLObject object) {
    recent_end_tag = false;
    return makeString(object, 0);
  }
  
  public static String makeString(FDLObject object) {
    return makeString(object, 0);
  }

  public static String makeString(FDLObject object, int indent) {
    Forget.about(indent);
    if (object instanceof FDLObjectIdentifier) {
      return ((FDLObjectIdentifier) object).identifier;
    } else if (object instanceof FDLObjectNumber) {
      return ((FDLObjectNumber) object).number;
    } else if (object instanceof FDLObjectString) {
      return ((FDLObjectString) object).string;
    } else if (object instanceof FDLObjectBraceList) {
      final FDLObjectBraceList brace_list = (FDLObjectBraceList) object;
      return renderList(brace_list, indent);
    } else if (object instanceof FDLObjectBracketList) {
      final FDLObjectBraceList bracket_list = (FDLObjectBraceList) object;
      return renderList(bracket_list, indent);
    } else if (object instanceof FDLObjectChain) {
      final FDLObjectChain chain = (FDLObjectChain) object;
      return renderChain(chain, indent);
    } else {
      throw new RuntimeException("Unknown FDL object");
    }
  }

  private static String renderList(FDLObjectBraceList list, int indent) {
    final StringBuffer sb = new StringBuffer();

    outputListStartTagAndAttributes(list, indent, sb);

    outputListChildren(list, indent, sb);

    outputListEndTag(list, indent, sb);

    return sb.toString();
  }
  
  public static String renderChain(FDLObjectChain chain, int indent) {
    final StringBuffer sb = new StringBuffer();
    FDLWriterStringUtilities.repeat(' ', indent);
    outputChainChildren(chain, sb, indent);

    return sb.toString();
  }

  private static void outputChainChildren(FDLObjectChain chain, final StringBuffer sb, int indent) {
    if (chain.children != null) {
      final int children_size = chain.children.size();
      for (int i = 0; i < children_size; i++) {
        final FDLObject node = (FDLObject) chain.children.elementAt(i);
        if (i > 0) {
          sb.append(chain.separator);
        } else {
          FDLWriterStringUtilities.indent(sb, indent);
        }
        sb.append(makeString(node, indent));
      }
    }
  }

  private static void outputListStartTagAndAttributes(FDLObjectBraceList list, final int indent,
      final StringBuffer sb) {
    Forget.about(indent);
    sb.append(list.open);
    if (list.newlines) {
      sb.append("\n");
      recent_end_tag = false;
    }
  }

  private static void outputListChildren(FDLObjectBraceList list, int indent, final StringBuffer sb) {
    if (list.children != null) {
      final int children_size = list.children.size();
      for (int i = 0; i < children_size; i++) {
        final FDLObject node = (FDLObject) list.children.elementAt(i);
        int spaces = 0;
        if (list.newlines) {
          spaces = indent + level;
        } else if (i > 0) {
          sb.append(list.separator);
          spaces = 0;
        }
        sb.append(makeString(node, spaces));
        if (list.newlines) {
          if (!recent_end_tag || (i < (children_size - 1))) {
            sb.append("\n");
          }
        }
      }
    }
    recent_end_tag = false;
  }

  private static void outputListEndTag(FDLObjectBraceList list, int indent, final StringBuffer sb) {
    if (list.newlines) {
      FDLWriterStringUtilities.indent(sb, indent);
    }
    sb.append(list.close);
    if (list.newlines) {
      //if (!recent_end_tag) {
        sb.append("\n");
      //}
      recent_end_tag = true;
    }
  }
}
