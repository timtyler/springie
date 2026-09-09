// This program has been placed into the public domain by its author.

package com.springie.presets;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.springie.io.in.ResourceLoader;
import com.tifsoft.Forget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadXMLModelIndexFile extends DefaultHandler {
  private static final Logger logger = LoggerFactory.getLogger(ReadXMLModelIndexFile.class);


  String leaf = "";

  String path;

  ArrayList<String> directories = new ArrayList<>();

  public String translate(String leaf, String source) throws IOException,
    SAXException {

    final XMLReader xr = new com.tifsoft.xml.driver.Driver();

    final ReadXMLModelIndexFile handler = new ReadXMLModelIndexFile();
    xr.setContentHandler(handler);
    xr.setErrorHandler(handler);
    handler.leaf = leaf;

    //Log.log("source:" + source);
    //Log.log("leaf:" + leaf);

    final Reader reader = new ResourceLoader().getReader(source);
    xr.parse(new InputSource(reader));

    logger.debug("handler.path:" + handler.path);

    return handler.path;
  }

  public void startDocument() {
    //Log.log("Start document");
  }

  public void endDocument() {
    //Log.log("End document");
  }

  public void startElement(String uri, String name, String element_name,
    Attributes atts) {
    Forget.about(name);
    boolean node = false;
    boolean leaf = false;

    if ("".equals(uri)) {
      node = "node".equals(element_name);
      leaf = "leaf".equals(element_name);

      final int n = atts.getLength();
      for (int i = 0; i < n; i++) {
        final String nam = atts.getLocalName(i);
        final String val = atts.getValue(i);

        if (node) {
          if ("name".equals(nam)) {
            this.directories.add(val);
          }
        }
        if (leaf) {
          if ("name".equals(nam)) {
            if (this.leaf.equals(val)) {
              this.path = "";
              for (int j = 0; j < this.directories.size(); j++) {
                this.path += this.directories.get(j) + "/";
              }
              this.path += this.leaf;
            }
          }
        }
      }
    }
  }

  public void endElement(String uri, String name, String element_name) {
    Forget.about(uri);
    Forget.about(name);
    Forget.about(element_name);

    if ("node".equals(element_name)) {
      this.directories.remove(this.directories.size() - 1);
    }
  }

  public void ignorableWhitespace(char[] ch, int start, int length) {
    characters(ch, start, length);
  }

  public void skippedEntity(String name) {
    Forget.about(name);
  }

  public void characters(char[] ch, int start, int length) {
    Forget.about(ch);
    Forget.about(start);
    Forget.about(length);
  }
}