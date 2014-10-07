// This is supporting software for CS321 Compilers and Language Design I
// Copyright (c) Portland State University
// 
// (For CS321 Fall 2014 - Jingke Li)
//

// A lexer for the ID token: ([A-Z]|[a-z])+.
// Skip white space and all other characters.
//
// (Version 2)
// - Show the use of a token object.
// - Show book-keeping line and column numbers.
//

import java.io.FileInputStream;

public class Lexer {

  // Internal token code
  static final int EOF = 0; // not used
  static final int ID = 1;

  // Token object
  static class Token {
    int kind; 		// token code
    int line;	   	// line number of token's first char
    int column;    	// column number of token's first char
    String lexeme;      // lexeme string
    
    public Token(int k, int l, int c, String lex) {
      kind = k; line = l; column = c; lexeme = lex; 
    }

    public String toString() {
      return lexeme;
    }
  }

  // File input handle
  static FileInputStream input = null;

  // Line and column numbers 
  static int linNum = 1;
  static int colNum = 0;

  // Read chars from a file; print out all tokens.
  //
  public static void main(String [] args) {
    try {
      if (args.length == 1) {
	input = new FileInputStream(args[0]);
	Token tkn;
      	int tknCnt = 0;
      	while ((tkn = nextToken()) != null) {
	  // ID token
	  System.out.println("(" + tkn.line + "," + tkn.column + ") " 
			     + tkn.kind + " " + tkn.lexeme);
	  tknCnt++;
	}
      	input.close();
      	System.out.println("Total: " + tknCnt + " tokens");
      } else {
	System.err.println("Need a file name as command-line argument.");
      }
    } catch (Exception e) {
      System.err.println(e);
    }
  }

  // Read next char from input; book-keep line and column numbers.
  //
  static int nextChar() throws Exception {
    int c = input.read();
    if (c == '\n') {
      linNum++;
      colNum = 0;
    } else {
      colNum++;
    }      
    return c;
  }

  // Recognize the next token and return its code.
  //
  static Token nextToken() throws Exception {
    StringBuilder buffer = new StringBuilder(); 
    int c = nextChar();
    for (;;) {
      switch (c) {

      case -1: 
	return null;
	
      // Skip whitespace
      case ' ':
      case '\t':
      case '\n':
      case '\r':
	c = nextChar();
	continue;

      default:
	if (isLetter(c)) {
	  int beginLine = linNum;
	  int beginColumn = colNum;
	  buffer.setLength(0);
	  do {
	    buffer.append((char) c);
	    c = nextChar();
	  } while (isLetter(c));
	  return new Token(ID, beginLine, beginColumn, buffer.toString());
	} else {
	  // skip other chars
	  c = nextChar();
	  continue;
	}	  
      }
    }	
  }
  
  // Return true if c is a letter.
  //
  private static boolean isLetter(int c) {
    return (('A' <= c) && (c <= 'Z') || ('a' <= c) && (c <= 'z'));
  }
}
