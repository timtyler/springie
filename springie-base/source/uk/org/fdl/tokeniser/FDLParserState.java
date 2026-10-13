package uk.org.fdl.tokeniser;

public class FDLParserState {
  public static FDLParserState in_whitespace = new FDLParserState();
  public static FDLParserState in_comment_sl = new FDLParserState();
  public static FDLParserState in_comment_ml = new FDLParserState();
  public static FDLParserState in_string_dq = new FDLParserState();
  public static FDLParserState in_string_sq = new FDLParserState();
  public static FDLParserState in_unicode_sequence = new FDLParserState();
  public static FDLParserState in_identifier = new FDLParserState();
  public static FDLParserState in_number = new FDLParserState();
  public static FDLParserState in_between_states = new FDLParserState();
}
