package uk.org.fdl.writer;

import uk.org.fdl.object.FDLObject;
import uk.org.fdl.object.FDLObjectBraceList;
import uk.org.fdl.object.FDLObjectBracketList;
import uk.org.fdl.object.FDLObjectChain;
import uk.org.fdl.object.FDLObjectIdentifier;
import uk.org.fdl.object.FDLObjectNumber;
import uk.org.fdl.object.FDLObjectString;


public final class FDLWriter {

  static boolean recent_end_tag;
  public static int level = 1;
  
  private FDLWriter() {
    //...
  }
  
  public static String toString(final FDLObject object) {
    recent_end_tag = false;
    return makeString(object, 0);
  }
  
  public static String makeString(final FDLObject object) {
    return makeString(object, 0);
  }

  public static String makeString(final FDLObject object, final int indent) {
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

  private static String renderList(final FDLObjectBraceList list, final int indent) {
    final StringBuilder sb = new StringBuilder();

    outputListStartTagAndAttributes(list, indent, sb);

    outputListChildren(list, indent, sb);

    outputListEndTag(list, indent, sb);

    return sb.toString();
  }
  
  public static String renderChain(final FDLObjectChain chain, final int indent) {
    final StringBuilder sb = new StringBuilder();
    FDLWriterStringUtilities.repeat(' ', indent);
    outputChainChildren(chain, sb, indent);

    return sb.toString();
  }

  private static void outputChainChildren(final FDLObjectChain chain, final StringBuilder sb, final int indent) {
    if (chain.children != null) {
      final int children_size = chain.children.size();
      for (int i = 0; i < children_size; i++) {
        final FDLObject node = (FDLObject) chain.children.get(i);
        if (i > 0) {
          sb.append(chain.separator);
        } else {
          FDLWriterStringUtilities.indent(sb, indent);
        }
        sb.append(makeString(node, indent));
      }
    }
  }

  private static void outputListStartTagAndAttributes(final FDLObjectBraceList list, final int indent,
      final StringBuilder sb) {
    sb.append(list.open);
    if (list.newlines) {
      sb.append("\n");
      recent_end_tag = false;
    }
  }

  private static void outputListChildren(final FDLObjectBraceList list, final int indent, final StringBuilder sb) {
    if (list.children != null) {
      final int children_size = list.children.size();
      for (int i = 0; i < children_size; i++) {
        final FDLObject node = (FDLObject) list.children.get(i);
        int spaces = 0;
        if (list.newlines) {
          spaces = indent + level;
        } else if (i > 0) {
          sb.append(list.separator);
          spaces = 0;
        }
        sb.append(makeString(node, spaces));
        if (list.newlines) {
          if (!recent_end_tag || i < (children_size - 1)) {
            sb.append("\n");
          }
        }
      }
    }
    recent_end_tag = false;
  }

  private static void outputListEndTag(final FDLObjectBraceList list, final int indent, final StringBuilder sb) {
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
