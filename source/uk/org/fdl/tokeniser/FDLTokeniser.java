package uk.org.fdl.tokeniser;

import uk.org.fdl.tokens.FDLElement;
import uk.org.fdl.tokens.FDLElementWithFixedText;
import uk.org.fdl.tokens.FDLElementWithVariableText;
import uk.org.fdl.tokens.FDLTokenTypes;

public class FDLTokeniser {
  FDLParserState state = FDLParserState.in_between_states;

  private String source;

  int index_char;

  int index_token;

  int line;

  int column;

  public FDLTokeniser() {
    // ...
  }

  public void setSource(String in) {
    this.source = in;
  }

  public boolean hasMoreTokens() {
    return this.index_char < this.source.length();
  }

  public FDLElement getNextToken() {
    final int start_line = this.line;
    final int start_column = this.column;
    this.state = FDLParserState.in_between_states;
    final int length = this.source.length();

    final StringBuffer token_text = new StringBuffer();

    while (this.index_char < length) {
      final char c = this.source.charAt(this.index_char++);
      // deal with new lines...
      if (c == '\n') {
        this.line++;
        this.column = 0;
      } else {
        this.column++;
      }
      // deal with the various states...
      if (this.state == FDLParserState.in_between_states) {
        if (c <= ' ') {
          this.state = FDLParserState.in_whitespace;
        } else if (isNumberStart(c)) {
          this.state = FDLParserState.in_number;
        } else if (c == '\'') {
          this.state = FDLParserState.in_string_sq;
        } else if (c == '"') {
          this.state = FDLParserState.in_string_dq;
        } else if (c == '<') {
          this.state = FDLParserState.in_comment_ml;
        } else if (c == '#') {
          this.state = FDLParserState.in_comment_sl;
        } else if (c == '+') {
          return new FDLElementWithFixedText(FDLTokenTypes.token_plus,
              start_line, start_column);
        } else if (c == '(') {
          return new FDLElementWithFixedText(FDLTokenTypes.token_bracket_open,
              start_line, start_column);
        } else if (c == ')') {
          return new FDLElementWithFixedText(FDLTokenTypes.token_bracket_close,
              start_line, start_column);
        } else if (c == '[') {
          return new FDLElementWithFixedText(FDLTokenTypes.token_bracket_square_open,
              start_line, start_column);
        } else if (c == ']') {
          return new FDLElementWithFixedText(FDLTokenTypes.token_bracket_square_close,
              start_line, start_column);
        } else if (c == '{') {
          return new FDLElementWithFixedText(FDLTokenTypes.token_brace_open,
              start_line, start_column);
        } else if (c == '}') {
          return new FDLElementWithFixedText(FDLTokenTypes.token_brace_close,
              start_line, start_column);
        } else if (c == ':') {
          return new FDLElementWithFixedText(FDLTokenTypes.token_colon,
              start_line, start_column);
        } else if (c == ',') {
          return new FDLElementWithFixedText(FDLTokenTypes.token_comma,
              start_line, start_column);
        } else if (c == '.') {
          return new FDLElementWithFixedText(FDLTokenTypes.token_dot,
              start_line, start_column);
        } else if (c == '=') {
          return new FDLElementWithFixedText(FDLTokenTypes.token_equals,
              start_line, start_column);
        } else {
          this.state = FDLParserState.in_identifier;
        }
        token_text.append(c);
      } else if (this.state == FDLParserState.in_identifier) {
        if (Character.isJavaIdentifierPart(c)) {
          token_text.append(c);
        } else {
          this.index_char--;
          return new FDLElementWithVariableText(
              FDLTokenTypes.token_identifier_fragment, token_text.toString(),
              start_line, start_column);
        }
      } else if (this.state == FDLParserState.in_number) {
        if (isNumberContinuation(c)) {
          token_text.append(c);
        } else {
          this.index_char--;
          return new FDLElementWithVariableText(
              FDLTokenTypes.token_number_fragment, token_text.toString(),
              start_line, start_column);
        }
      } else if (this.state == FDLParserState.in_string_sq) {
        token_text.append(c);
        if (c == '\'') {
          return new FDLElementWithVariableText(
              FDLTokenTypes.token_string_fragment_sq, token_text.toString(),
              start_line, start_column);
        }
      } else if (this.state == FDLParserState.in_string_dq) {
        token_text.append(c);
        if (c == '"') {
          return new FDLElementWithVariableText(
              FDLTokenTypes.token_string_fragment_dq, token_text.toString(),
              start_line, start_column);
        }
      } else if (this.state == FDLParserState.in_comment_ml) {
        token_text.append(c);
        if (c == '>') {
          return new FDLElementWithVariableText(
              FDLTokenTypes.token_comment_multi_line, token_text.toString(),
              start_line, start_column);
        }
      } else if (this.state == FDLParserState.in_comment_sl) {
        if (c == '\n') {
          token_text.append(c);
          return new FDLElementWithVariableText(
              FDLTokenTypes.token_comment_single_line, token_text.toString(),
              start_line, start_column);
        }
      } else if (this.state == FDLParserState.in_whitespace) {
        if (c <= ' ') {
          token_text.append(c);
        } else {
          this.index_char--;
          return new FDLElementWithVariableText(FDLTokenTypes.token_whitespace,
              token_text.toString(), start_line, start_column);
        }
      }
    }

    return new FDLElementWithVariableText(FDLTokenTypes.token_whitespace,
        token_text.toString(), start_line, start_column);
  }

  private boolean isNumberContinuation(char c) {
    if (c == '.') {
      return true;
    } else if (c == '_') {
      return true;
    } else if (c < '0') {
      return false;
    } else if (c <= '9') {
      return true;
    } else if (c < 'A') {
      return false;
    } else if (c <= 'Z') {
      return true;
    }

    return false;
  }

  private boolean isNumberStart(char c) {
    if (c == '-') {
      return true;
    } else if (c < '0') {
      return false;
    } else if (c > '9') {
      return false;
    }

    return true;
  }
}
