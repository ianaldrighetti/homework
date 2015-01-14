//----------------------------------------------------------------------
// CS321 Assignment 1 (Fall 2014)
//
// miniJava Lexer2 (Manual Implementation)
//----------------------------------------------------------------------

import java.io.*;

public class Lexer2 implements mjTokenConstants {

  static class LexError extends Exception {
    int line;
    int column;

    // need more code here ...
  }

  static class Token {
    int kind; 		// token code
    int line;	   	// line number of token's first char
    int column;    	// column number of token's first char
    String lexeme; 	// token lexeme
    
    // need more code here ...
  }

    // need more code here ...
}
