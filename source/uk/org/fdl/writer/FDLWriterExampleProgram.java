package uk.org.fdl.writer;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import com.tifsoft.fdl.old.ZFDLWriterAttribute;
import com.tifsoft.fdl.old.ZFDLWriterCharacters;
import com.tifsoft.fdl.old.ZFDLWriterNamedBraceList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FDLWriterExampleProgram {
  private static final Logger logger = LoggerFactory.getLogger(FDLWriterExampleProgram.class);

  Writer out;

  public void test(String filename) {
    try {
      this.out = new FileWriter(filename);
      final ZFDLWriterNamedBraceList uni = new ZFDLWriterNamedBraceList("test_tag_1");
      uni.add(new ZFDLWriterCharacters("CHARS 1"));
      uni.add(new ZFDLWriterAttribute("gravity", "5"));
      uni.add(new ZFDLWriterCharacters("CHARS 2"));
      uni.add(new ZFDLWriterAttribute("gravity_active", "true"));
      uni.add(new ZFDLWriterCharacters("CHARS 3"));
      final ZFDLWriterNamedBraceList ten = new ZFDLWriterNamedBraceList("test_tag_2");
      ten.add(new ZFDLWriterCharacters("CHARS 4"));
      ten.add(uni);
      ten.add(new ZFDLWriterCharacters("CHARS 5"));

      this.out.write(ten.makeString());
      this.out.flush();
    } catch (IOException e) {
      logger.debug("TT_Error (writeOutFile): " + e.toString());
    }
  }
}
