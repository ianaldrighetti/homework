// This is supporting software for CS321 Compilers and Language Design I
// Copyright (c) Portland State University
// 
// (For CS321 Fall 2014 - Jingke Li)
//

// A simple echo program, with file input.
//
//
import java.io.FileInputStream;

public class Lexer {
  public static void main(String [] args) {
    try {
      if (args.length == 1) {
	FileInputStream input = new FileInputStream(args[0]);
      	int c, charCnt = 0;
      	while ((c = input.read()) != -1) {	// read() returns -1 on EOF
	  System.out.print((char)c);
	  charCnt++;
      	}
      	input.close();
      	System.out.println("Total chars: " + charCnt);
      } else {
	System.err.println("Need a file name as command-line argument.");
      }
    } catch (Exception e) {
      System.err.println(e);
    }
  }
}
