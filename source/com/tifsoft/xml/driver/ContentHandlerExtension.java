package com.tifsoft.xml.driver;

import java.io.Writer;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;

public interface ContentHandlerExtension extends org.xml.sax.ContentHandler {
  Writer startDocument(Writer writer) throws SAXException;
  Writer startElement(String namespace_uri, String local_name, String name_q, Attributes atts, Writer writer) throws SAXException;
}
