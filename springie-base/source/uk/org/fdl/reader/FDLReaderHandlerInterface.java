// * Read in information from files...

package uk.org.fdl.reader;

import uk.org.fdl.object.FDLObjectBraceList;
import uk.org.fdl.object.FDLObjectBracketList;
import uk.org.fdl.object.FDLObjectChain;
import uk.org.fdl.object.FDLObjectIdentifier;
import uk.org.fdl.object.FDLObjectNumber;
import uk.org.fdl.object.FDLObjectString;

public interface FDLReaderHandlerInterface {
  void parseChain(FDLObjectChain chain);
  void parseBraceList(FDLObjectBraceList brace_list);
  void parseBracketList(FDLObjectBracketList bracket_list);
  void parseIdentifier(FDLObjectIdentifier identifier);
  void parseNumber(FDLObjectNumber number);
  void parseString(FDLObjectString string);
}
