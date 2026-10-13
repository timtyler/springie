package uk.org.fdl.tokens;

public class FDLTokenTypes {
  public static FDLTokenWithVariableText token_comment_multi_line = new FDLTokenWithVariableText(
      "CommentMultiLine");

  public static FDLTokenWithVariableText token_comment_single_line = new FDLTokenWithVariableText(
      "CommentSingleLine");

  public static FDLTokenWithVariableText token_number_fragment = new FDLTokenWithVariableText(
      "NumberFragment");

  public static FDLTokenWithVariableText token_identifier_fragment = new FDLTokenWithVariableText(
      "IdentifierFragment");

  public static FDLTokenWithVariableText token_string_fragment_sq = new FDLTokenWithVariableText(
      "StringFragmentSingleQuoted");

  public static FDLTokenWithVariableText token_string_fragment_dq = new FDLTokenWithVariableText(
      "StringFragmentDoubleQuoted");

  public static FDLTokenWithVariableText token_whitespace = new FDLTokenWithVariableText(
      "Whitespace");

  public static FDLTokenWithVariableText token_unrecognised = new FDLTokenWithVariableText(
      "Unrecognised");

  public static FDLTokenWithFixedText token_plus = new FDLTokenWithFixedText(
      "Plus", "+");

  public static FDLTokenWithFixedText token_colon = new FDLTokenWithFixedText(
      "Colon", ":");

  public static FDLTokenWithFixedText token_equals = new FDLTokenWithFixedText(
      "Equals", "=");

  public static FDLTokenWithFixedText token_comma = new FDLTokenWithFixedText(
      "Comma", ",");

  public static FDLTokenWithFixedText token_dot = new FDLTokenWithFixedText(
      "Dot", ".");

  public static FDLTokenWithFixedText token_brace_open = new FDLTokenWithFixedText(
      "BraceOpen", "{");

  public static FDLTokenWithFixedText token_brace_close = new FDLTokenWithFixedText(
      "BraceClose", "}");

  public static FDLTokenWithFixedText token_bracket_open = new FDLTokenWithFixedText(
      "BracketOpen", "(");

  public static FDLTokenWithFixedText token_bracket_close = new FDLTokenWithFixedText(
      "BraceClose", ")");

  public static FDLTokenWithFixedText token_bracket_square_open = new FDLTokenWithFixedText(
      "BracketSquareOpen", "[");

  public static FDLTokenWithFixedText token_bracket_square_close = new FDLTokenWithFixedText(
      "BraceSquareClose", "]");

  public static FDLTokenWithFixedText token_bracket_angle_open = new FDLTokenWithFixedText(
      "BracketAngleOpen", "<");

  public static FDLTokenWithFixedText token_bracket_angle_close = new FDLTokenWithFixedText(
      "BraceAngleClose", ">");
}
