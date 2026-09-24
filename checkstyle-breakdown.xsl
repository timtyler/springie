<?xml version="1.0" encoding="UTF-8"?>
<!-- Turns target/checkstyle-result.xml into a per-rule violation count,
     sorted by count descending, with a TOTAL line. Run from the build via
     maven-antrun-plugin (uses the JDK's built-in XSLT 1.0 processor). -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="text" encoding="UTF-8"/>
  <xsl:key name="by-rule" match="error" use="@source"/>

  <!-- Last dot-separated segment of a string (the check class name). -->
  <xsl:template name="last-segment">
    <xsl:param name="s"/>
    <xsl:choose>
      <xsl:when test="contains($s, '.')">
        <xsl:call-template name="last-segment">
          <xsl:with-param name="s" select="substring-after($s, '.')"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise><xsl:value-of select="$s"/></xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- "com.puppycrawl...checks.imports.UnusedImportsCheck" -> "UnusedImports". -->
  <xsl:template name="rule-name">
    <xsl:param name="source"/>
    <xsl:variable name="seg">
      <xsl:call-template name="last-segment">
        <xsl:with-param name="s" select="$source"/>
      </xsl:call-template>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="substring($seg, string-length($seg) - 4) = 'Check'">
        <xsl:value-of select="substring($seg, 1, string-length($seg) - 5)"/>
      </xsl:when>
      <xsl:otherwise><xsl:value-of select="$seg"/></xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="/">
    <xsl:text>Checkstyle violations by rule:&#10;</xsl:text>
    <xsl:for-each select="//error[generate-id() = generate-id(key('by-rule', @source)[1])]">
      <xsl:sort select="count(key('by-rule', @source))" data-type="number" order="descending"/>
      <xsl:variable name="padded" select="concat('     ', count(key('by-rule', @source)))"/>
      <xsl:value-of select="substring($padded, string-length($padded) - 4)"/>
      <xsl:text>  </xsl:text>
      <xsl:call-template name="rule-name">
        <xsl:with-param name="source" select="@source"/>
      </xsl:call-template>
      <xsl:text>&#10;</xsl:text>
    </xsl:for-each>
    <xsl:variable name="total" select="concat('     ', count(//error))"/>
    <xsl:value-of select="substring($total, string-length($total) - 4)"/>
    <xsl:text>  TOTAL&#10;</xsl:text>
  </xsl:template>
</xsl:stylesheet>
