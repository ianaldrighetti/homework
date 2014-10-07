//----------------------------------------------------------------------
// CS321 Assignment 1 (Fall 2014)
//
// miniJava Lexer2 (Manual Implementation)
//
// By Ian Aldrighetti
//----------------------------------------------------------------------

import java.io.*;

public class Lexer2 implements mjTokenConstants
{
	// A lexical error.
	static class LexError extends Exception
	{
		int line;
		int column;

		public LexError(int line, int column)
		{
			super("");
			this.line = line;
			this.column = column;
		}
		
		public LexError(String message, int line, int column)
		{
			super(message);
			this.line = line;
			this.column = column;
		}
		
		public int getLine()
		{
			return line;
		}
		
		public int getColumn()
		{
			return column;
		}
	}

	// A Token.
	static class Token
	{
		int kind; 		// token code
		int line;	   	// line number of token's first char
		int column;    	// column number of token's first char
		String lexeme; 	// token lexeme

		// need more code here ...
	}
	
	static class Buffer
	{
		int pos;
		String str;
		
		public Buffer(String str)
		{
			this.pos = 0;
			this.str = str;
		}
		
		public int next()
		{
			if (pos >= str.length())
			{
				return -1;
			}
			
			return (int)str.charAt(pos++);
		}
		
		public int peek(int offset)
		{
			if (pos + offset >= str.length())
			{
				return -1;
			}
			
			return (int)str.charAt(pos + offset);
		}
	}

    public void main(String[] args)
    {
		if (args.length == 0)
		{
			System.err.println("ERROR: An argument with the file name to parse is required.");
			return;
		}
		else if (args.length > 1)
		{
			System.err.println("ERROR: Only a single argument is allowed -- the file to parse.");
			return;
		}
		
		try
		{
			FileInputStream input = new FileInputStream(args[0]);
			
			StringBuilder buffer = new StringBuilder(input.available() * 2);
			int c;
			while((c = input.read()) != -1)
			{
				buffer.append((char)c);
			}
			
			Buffer b = new Buffer(buffer.toString());
			parse(b);
		}
		catch (Exception e)
		{
			System.err.println("ERROR: [" + e.getClass().getName() + "] " + e.getMessage());
		}
    }
    
    public void parse(Buffer buffer)
    {
    
    }
}
