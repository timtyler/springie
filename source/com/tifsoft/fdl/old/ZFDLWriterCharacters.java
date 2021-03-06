package com.tifsoft.fdl.old;

import uk.org.fdl.writer.FDLWriterInterface;
import uk.org.fdl.writer.FDLWriterStringUtilities;

public class ZFDLWriterCharacters implements FDLWriterInterface {
  String contents;

  public boolean newlines = true;

  public ZFDLWriterCharacters(String contents) {
    this.contents = contents;
  }

  public void add(String to_append) {
    this.contents += to_append;
  }

  public String makeString(int indent) {
    final StringBuffer sb = new StringBuffer();

    outputContents(indent, sb);

    return sb.toString();
  }

  public String makeString() {
    return makeString(0);
  }

  private void outputContents(final int indent, final StringBuffer sb) {
    if (this.contents != null) {
      FDLWriterStringUtilities.indent(sb, indent);
      sb.append(this.contents);
      if (this.newlines) {
        sb.append("\n");
      }
    }
  }
}
