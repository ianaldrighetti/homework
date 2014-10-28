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
	
	private Map<String, Integer> reservedMap;
	
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
			this.column = column;
			this.lexeme = lexeme;
		}
		
		public int kind()
		{
			return kind;
		}
		
		public int line()
		{
			return line;
		}
		
		public int column()
		{	
			return column;
		}
		
		public String lexeme()
		{
			return lexeme;
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
		
		public int current()
		{
			if (pos >= str.length())
			{
				return -1;
			}
			
			return (int)str.charAt(pos);
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
		
		public void setPos(int pos)
		{
			if (pos >= str.length())
			{
				pos = str.length();
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

    public static void main(String[] args) throws Exception
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
			Lexer2 lexer = new Lexer2();
			List<Token> tokens = lexer.parse(b);
			
			displayTokens(tokens);
		}
		catch (LexError e)
		{
			System.err.println("!!! miniJava Lexical Error: " + e.getMessage());
			System.err.println("    Line: " + e.getLine() + ", column: " + e.getColumn());
		}
		catch (Exception e)
		{
			System.err.println("ERROR: [" + e.getClass().getName() + "] " + e.getMessage());
			throw e;
		}
    }
    
    public static void displayTokens(List<Token> tokens) throws Exception
    {
		System.out.println(getTokenOutput(tokens));
    }
    
    public static String getTokenOutput(List<Token> tokens) throws Exception
    {
		StringBuilder buffer = new StringBuilder();
		
		for (Token token : tokens)
		{
			buffer.append(getTokenRepresentation(token)).append("\n");
		}
		
		buffer.append("Total: ").append(tokens.size()).append(" tokens");
		
		return buffer.toString();
    }
    
    private static String getTokenRepresentation(Token token) throws Exception
    {
		StringBuilder buffer = new StringBuilder();
		buffer.append("(").append(token.line()).append(",").append(token.column()).append(")\t");
		
		if (token.kind() == ID)
		{
			buffer.append("ID(").append(token.lexeme()).append(")");
		}
		else if (token.kind() == INTLIT)
		{
			long val = 0;
			
			try
			{
				val = Long.parseLong(token.lexeme());
			}
			catch (NumberFormatException e)
			{
				throw new LexError("The value " + token.lexeme() + " is not a number.", token.line(), token.column());
			}
			
			if (val > Integer.MAX_VALUE)
			{
				throw new LexError("The integer " + val + " is too large (must not be larger than 2^31 - 1).", token.line(), token.column());
			}
			
			buffer.append("INTLIT(").append(val).append(")");
		}
		else if (token.kind() == STRLIT)
		{
			buffer.append("STRLIT(\"").append(token.lexeme()).append("\")");
		}
		else
		{
			buffer.append(token.lexeme());
		}
		
		return buffer.toString();
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
    	reservedKeywords.add("this");
    	
    	reservedMap = new HashMap<String, Integer>();
    	reservedMap.put("class", CLASS);
    	reservedMap.put("extends", EXTENDS);
    	reservedMap.put("static", STATIC);
    	reservedMap.put("public", PUBLIC);
    	reservedMap.put("void", VOID);
    	reservedMap.put("int", INT);
    	reservedMap.put("boolean", BOOLEAN);
    	reservedMap.put("new", NEW);
    	reservedMap.put("if", IF);
    	reservedMap.put("else", ELSE);
    	reservedMap.put("while", WHILE);
    	reservedMap.put("return", RETURN);
    	reservedMap.put("main", MAIN);
    	reservedMap.put("true", TRUE);
    	reservedMap.put("false", FALSE);
    	reservedMap.put("String", STRING);
    	reservedMap.put("System", SYSTEM);
    	reservedMap.put("out", OUT);
    	reservedMap.put("println", PRINTLN);
    	reservedMap.put("this", THIS);
    	
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
    public List<Token> parse(Buffer buffer) throws LexError, Exception
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
    		
    		Token token = tokenize((char)c, buffer);
    		
    		// It might be empty (a comment, perhaps).
    		if (token == null)
    		{
    			continue;
    		}
    		
    		tokens.add(token);
    	}
    	
    	return tokens;
    }
    
    private Token tokenize(char ch, Buffer buffer) throws Exception
    {
		Token token;
		
    	// Is it a string?
    	//done
    	if (ch == '"')
    	{
    		return getStringLiteral(ch, buffer);
    	}
    	//done
    	else if (Character.isDigit(ch))
    	{
    		return getIntegerLiteral(ch, buffer);
    	}
    	//done
    	else if (buffer.peek(0) > -1 && ((ch == '/' && ((char)buffer.peek(0)) == '/') || (ch == '/' && ((char)buffer.peek(0)) == '*')))
    	{
    		handleComment(ch, buffer);
    		
    		return null;
    	}
    	//done
    	else if (isOperatorOrDelimiter(ch, buffer.peek(0)))
    	{
    		return getOperatorOrDelimiter(ch, buffer);
    	}
    	//done
    	else if ((token = getReservedKeyword(ch, buffer)) != null)
    	{
    		return token;
    	}
    	else if (Character.isLetter(ch))
    	{
    		return getIdentifier(ch, buffer);
    	}
    	else
    	{
			throw new LexError("Unrecognized character: " + ch, lineNumber, columnNumber);
    	}
    }
    
    private Token getStringLiteral(char ch, Buffer buffer) throws LexError
    {
		int originalColumnNumber = columnNumber;
		columnNumber++;
		StringBuilder strBuffer = new StringBuilder();
		
		int c;
		boolean ended = false;
		while((c = buffer.next()) != -1)
		{
			columnNumber++;
			
			// We do not allow \n or \r.
			if (c == '\r')
			{
				throw new LexError("Unexpected carriage return in string (strings may not contain this character).", lineNumber, columnNumber);
			}
			else if (c == '\n')
			{
				throw new LexError("Unexpected line feed in string (strings may not contain this character).", lineNumber, columnNumber);
			}
			
			// No double quotes are allowed in strings, so this is it.
			if (c == '"')
			{
				ended = true;
				break;
			}
		
			strBuffer.append((char)c);
		}
		
		if (!ended)
		{
			throw new LexError("Unterminated string.", lineNumber, originalColumnNumber);
		}
		
		return new Token(STRLIT, lineNumber, originalColumnNumber, strBuffer.toString());
    }
    
    private Token getIdentifier(char ch, Buffer buffer)
    {
		int originalColumnNumber = columnNumber;
		StringBuilder strBuffer = new StringBuilder();
		strBuffer.append(ch);
		
		int offset = 0;
		while (true)
		{
			if (!Character.isLetter(buffer.peek(offset)) && !Character.isDigit(buffer.peek(offset)))
			{
				break;
			}
			
			strBuffer.append((char)buffer.peek(offset++));
		}
		
		columnNumber += strBuffer.toString().length();
		buffer.setPos(buffer.getPos() + strBuffer.toString().length() - 1);
		return new Token(ID, lineNumber, originalColumnNumber, strBuffer.toString());
    }
    
    private Token getReservedKeyword(char ch, Buffer buffer)
    {
		int originalColumnNumber = columnNumber;
		StringBuilder strBuffer = new StringBuilder();
		strBuffer.append(ch);
		
		int offset = 0;
		while (true)
		{
			if (!Character.isLetter(buffer.peek(offset)) && !Character.isDigit(buffer.peek(offset)))
			{
				break;
			}
			
			strBuffer.append((char)buffer.peek(offset++));
		}
		
		if (!reservedKeywords.contains(strBuffer.toString()))
		{
			return null;
		}
		
		columnNumber += strBuffer.toString().length();
		
		buffer.setPos(buffer.getPos() + strBuffer.toString().length() - 1);
		return new Token(reservedMap.get(strBuffer.toString()), lineNumber, originalColumnNumber, strBuffer.toString());
    }
    
    private void handleComment(char ch, Buffer buffer) throws LexError
    {
		int originalLineNumber = lineNumber;
		int originalColumnNumber = columnNumber;
		
		// Moves us passed the first /.	
		columnNumber++;
		
		// This will also get passed the / as well.
		if (buffer.next() == '*')
		{
			// Multiline, keep going until we get to */
			boolean finished = false;
			while (true)
			{
				if (buffer.next() == '*' && buffer.peek(0) == '/')
				{
					columnNumber += 2;
					
					// We peeked at the /, make it official.
					buffer.next();
					finished = true;
					break;
				}
				
				if (buffer.current() == '\n')
				{
					lineNumber++;
					columnNumber = 1;
					continue;
				}
				
				if (buffer.current() == -1)
				{
					break;
				}
				
				columnNumber++;
			}
			
			if (!finished)
			{
				throw new LexError("Unterminated multiline comment.", originalLineNumber, originalColumnNumber);
			}
			
			return;
		}
		
		while (true)
		{
			if (buffer.next() == '\n')
			{
				lineNumber++;
				columnNumber = 1;
				break;
			}
		}
    }
    
    private Token getIntegerLiteral(char ch, Buffer buffer)
    {
		int originalColumnNumber = columnNumber;
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
    		
    		intBuffer.append((char)cur);
    		pos++;
    	}
    	
    	buffer.setPos(pos);
    	
    	columnNumber += intBuffer.toString().length();
    	return new Token(INTLIT, lineNumber, originalColumnNumber, intBuffer.toString());
    }
    
    private Token getOperatorOrDelimiter(char ch, Buffer buffer)
    {
		int originalColumnNumber = columnNumber;
		
    	String str = String.valueOf(ch) + (buffer.peek(0) == -1 ? "" : String.valueOf((char)buffer.peek(0)));
		
    	if (getOperatorMap().containsKey(str))
    	{
			columnNumber += str.length();
			buffer.next();
			
			return new Token(getOperatorMap().get(str), lineNumber, originalColumnNumber, str);
    	}
    	
    	str = String.valueOf(ch);
    	columnNumber++;
    	
    	return new Token((int)ch, lineNumber, originalColumnNumber, str);
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
    	if (operators.contains(buffer.toString()))
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
