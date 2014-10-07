//----------------------------------------------------------------------
// CS321 Assignment 1 (Fall 2014)
//
// miniJava Lexer2 (Manual Implementation)
//
// By Ian Aldrighetti
//----------------------------------------------------------------------

import java.io.*;
import java.util.*;

public class Lexer2 implements mjTokenConstants
{
	// A set containing reserved keywords.
	private Set<String> reservedKeywords;
	
	private Set<String> operators;
	
	private Set<String> delimiters;
	
	private int lineNumber;
	
	private int columnNumber;
	
	private Map<String, Integer> operatorMap;
	
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

		public Token(int kind, int line, int column, String lexeme)
		{
			this.kind = kind;
			this.line = line;
			this.column = line;
			this.lexeme = lexeme;
		}
	}
	
	// Custom buffer class.
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
		
		public int getPos()
		{
			return pos;
		}
		
		public int setPos(int pos)
		{
			if (pos >= str.length())
			{
				throw new IllegalArgumentException("Attempting to set pos to " + pos + ", it is out of range (max: " + (str.length() - 1) + ").");
			}
			
			this.pos = pos;
		}
		
		public int charAt(int pos)
		{
			if (pos < 0 || pos >= str.length())
			{
				throw new IllegalArgumentException("Attempting to get char at " + pos + ", it is out of range (max: " + (str.length() - 1) + ").");
			}
			
			return (int)str.charAt(pos);
		}
		
		public int length()
		{
			return str.length();
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
			List<Token> tokens = parse(b);
		}
		catch (Exception e)
		{
			System.err.println("ERROR: [" + e.getClass().getName() + "] " + e.getMessage());
		}
    }
    
    // Loads the reserved keywords into a set, if necessary.
    private void loadReservedKeywords()
    {
    	if (reservedKeywords != null)
    	{
    		return;
    	}
    	
    	reservedKeywords = new HashSet<String>();
    	reservedKeywords.add("class");
    	reservedKeywords.add("extends");
    	reservedKeywords.add("static");
    	reservedKeywords.add("public");
    	reservedKeywords.add("void");
    	reservedKeywords.add("int");
    	reservedKeywords.add("boolean");
    	reservedKeywords.add("new");
    	reservedKeywords.add("if");
    	reservedKeywords.add("else");
    	reservedKeywords.add("while");
    	reservedKeywords.add("return");
    	reservedKeywords.add("main");
    	reservedKeywords.add("true");
    	reservedKeywords.add("false");
    	reservedKeywords.add("String");
    	reservedKeywords.add("System");
    	reservedKeywords.add("out");
    	reservedKeywords.add("println");
    	
    	operators = new HashSet<String>();
    	operators.add("+");
    	operators.add("-");
    	operators.add("*");
    	operators.add("/");
    	operators.add("&&");
    	operators.add("||");
    	operators.add("!");
    	operators.add("==");
    	operators.add("!=");
    	operators.add("<");
    	operators.add("<=");
    	operators.add(">");
    	operators.add(">=");
    	
    	// MUST NOT CONTAIN MORE THAN 2 CHARACTERS PER DELIMITER! (see isOperatorOrDelimiter).
    	delimiters = new HashSet<String>();
    	delimiters.add("=");
    	delimiters.add(";");
    	delimiters.add(",");
    	delimiters.add(".");
    	delimiters.add("(");
    	delimiters.add(")");
    	delimiters.add("[");
    	delimiters.add("]");
    	delimiters.add("{");
    	delimiters.add("}");
    }
    
    /*
     Identifiers - excluding reserved, starts with a letter, can contain letters/numbers.
     
     `Integer - Only digits, must be between 0 and (2^31) - 1, negative just has - (operator) in 				front.
     `String - begin, end with quote quote ("), cannot contain ", \r, \n (verify this).
     
     Comments - // or multi-line (/ * * / [no space]), cannot be nested.
     
     Operators: +, -, *, /, &&, ||, !, ==, !=, <, <=, >, >=
     
     Delimiter: =, ;, ,, ., (, ), [, ], {, }
    */
    
    // Parses the file, within the buffer.
    public List<Token> parse(Buffer buffer) throws LexError
    {
    	lineNumber = 1;
    	columnNumber = 1;
    	
    	List<Token> tokens = new ArrayList<Token>();
    	
    	loadReservedKeywords();
    	
    	int c;
    	while ((c = buffer.next()) != -1)
    	{
    		char ch = (char)c;
    		
    		// If it is a space, newline, etc., skip it!
    		if (isSkippedChar(ch))
    		{
    			// Increment the line number, though.
    			lineNumber = ch == '\n' ? lineNumber + 1 : lineNumber;
    			columnNumber = ch == '\n' ? 1 : columnNumber + 1;
    			continue;
    		}
    		
    		Token token = tokenize(c, buffer);
    		
    		// It might be empty (a comment, perhaps).
    		if (token == null)
    		{
    			continue;
    		}
    		
    		tokens.add(token);
    	}
    }
    
    private Token tokenize(char ch, Buffer buffer)
    {
    	// Is it a string?
    	if (ch == '"')
    	{
    		return getStringLiteral(ch, buffer);
    	}
    	//done
    	else if (Character.isDigit(ch))
    	{
    		// TODO: Remember, check it's size!
    		return getIntegerLiteral(ch, buffer);
    	}
    	else if (buffer.peek(1) > -1 && ((ch == '/' && buffer.peek(1) == '/') || (ch == '/' && buffer.peek(1) == '*')))
    	{
    		// !!! TODO: Line number may need to be handled differently (multiline comment).
    		handleComment(ch, buffer);
    		
    		return null;
    	}
    	else if (isOperatorOrDelimiter(ch, buffer.peek(1)))
    	{
    		return getOperatorOrDelimiter(ch, buffer);
    	}
    	else if (isReservedKeyword(ch, buffer))
    	{
    		return getReservedKeyword(ch, buffer);
    	}
    	else
    	{
    		// Must be an identifier.
    	}
    }
    
    private Token getIntegerLiteral(char ch, Buffer buffer)
    {
    	// Set the position to the original character (ch).
    	int pos = buffer.getPos() - 1;
    	
    	StringBuilder intBuffer = new StringBuilder();
    	int cur;
    	while(pos < buffer.length())
    	{
    		cur = buffer.charAt(pos);
    		
    		if (!Character.isDigit((char)cur))
    		{
    			// !!! TODO: Might need pos--?
    			break;
    		}
    		
    		intBuffer.append((char)ch);
    		pos++;
    	}
    	
    	buffer.setPos(pos);
    	
    	columnNumber += intBuffer.toString().length();
    	return new Token(INTLIT, lineNumber, columnNumber, intBuffer.toString());
    }
    
    private Token getOperatorOrDelimiter(char ch, Buffer buffer)
    {
    	String str = String.valueOf(ch) + (buffer.peek(1) == -1 ? "" : String.valueOf(buffer.peek(1)));
    
    	// !!! LEFT OFF HERE TODO
    }
    
    private boolean isOperatorOrDelimiter(char ch, int nextCh)
    {
    	StringBuilder buffer = new StringBuilder(2);
    	buffer.append(ch);
    	
    	if (nextCh > -1)
    	{
    		buffer.append((char)nextCh);
    	}
    	
    	// Only operators contain entries with 2 characters.
    	if (operators.contain(buffer.toString()))
    	{
    		return true;
    	}
    	else if (operators.contains(String.valueOf(ch)) || delimiters.contains(String.valueOf(ch)))
    	{
    		return true;
    	}
    	
    	return false;
    }
    
    private Map<String, Integer> getOperatorMap()
    {
    	if (operatorMap != null)
    	{
    		return operatorMap;
    	}
    	
    	operatorMap = new HashMap<String, Integer>();
    	
    	operatorMap.put("==", EQ);
    	operatorMap.put("!=", NEQ);
    	operatorMap.put("<=", LE);
    	operatorMap.put(">=", GE);
    	operatorMap.put("&&", AND);
    	operatorMap.put("||", OR);
    	
    	return operatorMap;
    }
    
    // Determines if ch is \n, \r, \t or a space.
    private boolean isSkippedChar(char ch)
    {
    	return ch == '\n' || ch == '\r' || ch == '\t' || ch == ' ';
    }
}