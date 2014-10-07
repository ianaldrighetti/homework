// This is supporting software for CS321 Compilers and Language Design I
// Copyright (c) Portland State University
// 
// (For CS321 Fall 2014 - Jingke Li)
//

// A lexer for the two tokens: "begin" and "end". 
// Ignore all other characters.
//
// - Show basic pattern matching.
//

import java.io.FileInputStream;

public class Lexer {

  // Internal token code
  static final int EOF = 0;
  static final int BEGIN = 1;
  static final int END = 2;

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
	  switch(tknCode) {
	  case BEGIN: System.out.println("BEGIN"); break;
	  case END:   System.out.println("END"); break;
	  }
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
    int c = input.read();
    for (;;) {
      switch (c) {
      case -1: 
	return EOF;

      case 'b':
	// read the next four chars
	if ((c=input.read()) == 'e' && (c=input.read()) == 'g' && 
	    (c=input.read()) == 'i' && (c=input.read()) == 'n')
	  return BEGIN;
	break;

      case 'e':
	// read the next two chars
	if ((c=input.read()) == 'n' && (c=input.read()) == 'd')
	  return END;
	break;

      default:
	// skip all other chars
	c = input.read();
	continue;
      }	
    }
  }
}
