// This is supporting software for CS321 Compilers and Language Design I
// Copyright (c) Portland State University
// 
// (For CS321 Fall 2014 - Jingke Li)
//

// A lexer for the ID token: ([A-Z]|[a-z])+. 
// Skip white space and all other characters.
//
// (Version 1)
// - Show the use of input buffer.
//

import java.io.FileInputStream;

public class Lexer {

  // Internal token code
  static final int EOF = 0;
  static final int ID = 1;

  // Lexeme string
  static String lexeme = null;			     

  // File input handle
  static FileInputStream input = null;

  // Read chars from a file; print out all tokens.
  //
  public static void main(String [] args) {
    try {
      if (args.length == 1) {
	input = new FileInputStream(args[0]);
      	int tknCode, tknCnt = 0;
      	while ((tknCode = nextToken()) != EOF) {
	  // tknCode == ID
	  System.out.println(lexeme);
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

  // Read chars from input; recognize the next token and return its code.
  //
  static int nextToken() throws Exception {
    StringBuilder buffer = new StringBuilder(); 
    int c = input.read();
    for (;;) {
      switch (c) {

      case -1: 
	return EOF;
	
      // Skip whitespace
      case ' ':
      case '\t':
      case '\n':
      case '\r':
	c = input.read();
	continue;

      default:
	if (isLetter(c)) {
	  // save ID token's lexeme
	  buffer.setLength(0);
	  do {
	    buffer.append((char) c);
	    c = input.read();
	  } while (isLetter(c));
	  lexeme = buffer.toString();
	  return ID;
	} else {
	  // skip other chars
	  c = input.read();
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
