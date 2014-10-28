//----------------------------------------------------------------------
// CS321 Assignment 1 (Fall 2014)
//
// miniJava Token Definitions (Manual Implementation)
//----------------------------------------------------------------------

public interface mjTokenConstants {
    int EOF      = 0;  
    int ID       = 1;  
    int INTLIT   = 2;  
    int STRLIT   = 3;  
    int TRUE     = 4;  	// "true" 
    int FALSE    = 5;  	// "false"
    int CLASS    = 6;  	// "class"
    int EXTENDS  = 7;  	// "extends"
    int STATIC   = 8;  	// "static"
    int PUBLIC   = 9;  	// "public"
    int VOID     = 10; 	// "void"
    int MAIN     = 11; 	// "main"
    int INT      = 12; 	// "int"
    int STRING   = 13; 	// "String"
    int BOOLEAN  = 14; 	// "boolean"
    int NEW      = 15; 	// "new"
    int THIS     = 16; 	// "this"
    int IF       = 17; 	// "if"
    int ELSE     = 18; 	// "else"
    int SYSTEM   = 19; 	// "System"
    int OUT      = 20;  // "out"
    int PRINTLN  = 21;  // "println"
    int WHILE    = 22; 	// "while"
    int RETURN   = 23;	// "return"
    int EQ       = 24;  // ==
    int NEQ      = 25;  // !=
    int LE       = 26;  // <=
    int GE       = 27;  // >=
    int AND      = 28;  // &&
    int OR       = 29;  // ||

    // The remaining single-character operators/delimiters
    // are represented by their ASCII code (which have values
    // greater than 32).
    //
    // "+" | "-" | "*" | "/" | "!" | "<" | ">" | 
    // "=" | ";" | "," | "." | "(" | ")" | "[" | "]" | "{" | "}"
}
