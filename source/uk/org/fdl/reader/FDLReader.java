// * Read in information from files...

package uk.org.fdl.reader;

import java.util.Vector;

import uk.org.fdl.tokeniser.FDLTokeniser;
import uk.org.fdl.tokens.FDLElement;

import com.springie.utilities.log.Log;
import com.tifsoft.Forget;

public final class FDLReader {

  public static void main(String[] args) {
    Forget.about(args);
    Log.log(test("foo bar() {  0 10 1.9 -3 <Comment> 'aoe' \"aseo\" }"));
  }

  private FDLReader() {
    // ...
  }

  public static String test(String in) {
    final StringBuffer out = new StringBuffer();

    final Vector tokens = new Vector();

    final FDLTokeniser token_reader = new FDLTokeniser();
    token_reader.setSource(in);

    while (token_reader.hasMoreTokens()) {
      final FDLElement element = token_reader.getNextToken();
      tokens.add(element);
    }

    dumpOutTokens(out, tokens);

    return out.toString();
  }

  private static void dumpOutTokens(StringBuffer out, Vector tokens) {
    final int size = tokens.size();
    for (int i = 0; i < size; i++) {
      final FDLElement array_element = (FDLElement) tokens.elementAt(i);
      out.append("(" + array_element.line + "," + array_element.column + ") - "
          + array_element.type.name + " - <" + array_element.getText() + ">\n");
    }
  }
}