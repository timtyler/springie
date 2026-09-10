package com.springie.io.in;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;

import com.springie.FrEnd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceLoader {
  private static final Logger logger = LoggerFactory.getLogger(ResourceLoader.class);

  public String getResourceAsString(Class<?> base, String name) throws IOException {
    String output;

    //Log.log("Starting to load " + name + ".");
    final InputStream in = getInputStream(base, name);
    if (in == null) {
      logger.debug("Base class: <" + base + ">");
      throw new FileNotFoundException("File not found: <" + name + ">");
    }

    output = getStringFromInputStream(in);

    return output;
  }

  private String getStringFromInputStream(final InputStream in) {
    String output = null;
    ByteArrayOutputStream bytes;

    bytes = new ByteArrayOutputStream();
    final int array_size = 1024; // choose a size...
    final byte[] array = new byte[array_size];

    int rb;

    try {
      while ((rb = in.read(array, 0, array_size)) > -1) {
        bytes.write(array, 0, rb);
      }

      bytes.close();

      output = bytes.toString();

      in.close();
    } catch (IOException e) {
      logger.error("Failed to load resource", e);
    }
    return output;
  }

  String getStringFromReader(Reader r) {
    final StringBuilder output = new StringBuilder();

    final int array_size = 1024; // choose a size...
    final char[] array = new char[array_size];

    int rb;

    try {
      while ((rb = r.read(array, 0, array_size)) > -1) {
        output.append(array, 0, rb);
      }

      r.close();
    } catch (IOException e) {
      logger.error("Failed to load resource", e);
    }

    return output.toString();
  }

  private InputStream getInputStream(Class<?> base, String location) {
    if (isURL(location)) {
      try {
        return getResourceFromURL(location);
      } catch (IOException e) {
        logger.error("Failed to load URL: <" + location + ">", e);

        return null;
      }
    }

    return base.getResourceAsStream(location);
  }

  private InputStream getResourceFromURL(String location) throws IOException {
    final URL url = new URL(location);

    return url.openStream();
  }

  private String getResourceAsStringHelper(Class<?> base, String location)
      throws IOException {
    if (isResource(location)) {
      return getResourceAsString(base, location.substring(11));
    }

    if (isArchive(location)) {
      return new ZipLoader().getZIPURLAsString(location);
    }

    return getResourceAsString(base, location);
  }

  public Reader getReader(String location) throws IOException {
    if (isFile(location)) {
      if (isArchive(location)) {
        final String s = new ZipLoader().getZIPFileAsString(location.substring(7));

        return new StringReader(s);
      }

      return new FileReader(location.substring(7));
    }

    if (isResource(location)) {
      final String s = getResourceAsStringHelper(FrEnd.class, location);

      return new StringReader(s);
    }

    if (isURL(location)) {
      if (isArchive(location)) {
        final String s = new ZipLoader().getZIPURLAsString(location);

        return new StringReader(s);
      }
      final String s = getURLAsString(location);

      return new StringReader(s);
    }

    throw new IOException("Cannot handle location: <" + location + ">");
  }

  private String getURLAsString(String url) throws IOException {
    final InputStream is = getResourceFromURL(url);
    return getStringFromInputStream(is);
  }

  boolean isURL(String location) {
    return location.startsWith("http://");
  }

  boolean isArchive(String location) {
    return location.endsWith(".zip");
  }

  boolean isFile(String location) {
    return location.startsWith("file://");
  }

  boolean isResource(String location) {
    return location.startsWith("resource://");
  }
}