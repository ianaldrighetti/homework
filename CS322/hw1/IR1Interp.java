// 
// A starting version of IR1 interpreter. (For CS322 W15 Assignment 1)
//
//
import java.util.*;
import java.io.*;

import ir1.*;

public class IR1Interp
{
	
	static class IntException extends Exception
	{
		private static final long serialVersionUID = 1L;

		public IntException(String msg)
		{
			super(msg);
		}
	}
	
	// -----------------------------------------------------------------
	// Value representation
	// -----------------------------------------------------------------
	//
	abstract static class Val
	{
	}
	
	// Integer values
	//
	static class IntVal extends Val
	{
		int i;
		
		IntVal(int i)
		{
			this.i = i;
		}
		
		public String toString()
		{
			return "" + i;
		}
	}
	
	// Boolean values
	//
	static class BoolVal extends Val
	{
		boolean b;
		
		BoolVal(boolean b)
		{
			this.b = b;
		}
		
		public String toString()
		{
			return "" + b;
		}
	}
	
	// String values
	//
	static class StrVal extends Val
	{
		String s;
		
		StrVal(String s)
		{
			this.s = s;
		}
		
		public String toString()
		{
			return s;
		}
	}
	
	// A special "undefined" value
	//
	static class UndVal extends Val
	{
		public String toString()
		{
			return "UndVal";
		}
	}
	
	// -----------------------------------------------------------------
	// Environment representation
	// -----------------------------------------------------------------
	//
	// Think of how to organize environments.
	//
	// The following environments are shown in the lecture for use in
	// an IR0 interpreter:
	//
	// HashMap<String,Integer> labelMap; // label table
	// HashMap<Integer,Val> tempMap; // temp table
	// HashMap<String,Val> varMap; // var table
	//
	// For IR1, they need to be managed at per function level.
	//
	
	// ... code needed ...
	
	// -----------------------------------------------------------------
	// Global variables and constants
	// -----------------------------------------------------------------
	//
	// These variables and constants are for your reference only.
	// You may decide to use all of them, some of these, or not at all.
	//
	
	// Function lookup table
	// - maps function names to their AST nodes
	//
	static HashMap<String, IR1.Func> funcMap;
	
	// Heap memory
	// - for handling 'malloc'ed data
	// - you need to define alloc and access methods for it
	//
	static ArrayList<Val> heap;
	
	// Return value
	// - for passing return value from callee to caller
	//
	static Val retVal;
	
	// Execution status
	// - tells whether to continue with the nest inst, to jump to
	// a new target inst, or to return to the caller
	//
	static final int CONTINUE = 0;
	static final int RETURN = -1;
	
	// -----------------------------------------------------------------
	// The main method
	// -----------------------------------------------------------------
	//
	// 1. Open an IR1 program file.
	// 2. Call the IR1 AST parser to read in the program and
	// convert it to an AST (rooted at an IR1.Program node).
	// 3. Invoke the interpretation process on the root node.
	//
	@SuppressWarnings ("static-access")
	public static void main(String[] args) throws Exception
	{
		if (args.length == 1)
		{
			FileInputStream stream = new FileInputStream(args[0]);
			IR1.Program p = new ir1Parser(stream).Program();
			stream.close();
			IR1Interp.execute(p);
		}
		else
		{
			System.out.println("You must provide an input file name.");
		}
	}
	
	// -----------------------------------------------------------------
	// Top-level IR nodes
	// -----------------------------------------------------------------
	//
	
	// Program ---
	// Func[] funcs;
	//
	// 1. Establish the function lookup map
	// 2. Lookup 'main' in funcMap, and
	// 3. start interpreting from main's AST node
	//
	public static void execute(IR1.Program n) throws Exception
	{
		funcMap = new HashMap<String, IR1.Func>();
		storage = new ArrayList<Val>();
		retVal = Val.Undefined;
		for (IR1.Func f : n.funcs)
			funcMap.put(f.name, f);
		execute(funcMap.get("main"));
	}
	
	// Func ---
	// String name;
	// Var[] params;
	// Var[] locals;
	// Inst[] code;
	//
	// 1. Collect label decls information and store them in
	// a label-lookup table for later use.
	// 2. Execute the fetch-and-execute loop.
	//
	static void execute(IR1.Func n) throws Exception
	{
		
		// ... code needed ...
		
		// The fetch-and-execute loop
		int idx = 0;
		while (idx < n.code.length)
		{
			int next = execute(n.code[idx]);
			if (next == CONTINUE)
				idx++;
			else if (next == RETURN)
				break;
			else
				idx = next;
		}
	}
	
	// Dispatch execution to an individual Inst node.
	//
	static int execute(IR1.Inst n) throws Exception
	{
		if (n instanceof IR1.Binop)
			return execute((IR1.Binop) n);
		if (n instanceof IR1.Unop)
			return execute((IR1.Unop) n);
		if (n instanceof IR1.Move)
			return execute((IR1.Move) n);
		if (n instanceof IR1.Load)
			return execute((IR1.Load) n);
		if (n instanceof IR1.Store)
			return execute((IR1.Store) n);
		if (n instanceof IR1.Jump)
			return execute((IR1.Jump) n);
		if (n instanceof IR1.CJump)
			return execute((IR1.CJump) n);
		if (n instanceof IR1.Call)
			return execute((IR1.Call) n);
		if (n instanceof IR1.Return)
			return execute((IR1.Return) n);
		if (n instanceof IR1.LabelDec)
			return CONTINUE;
		throw new IntException("Unknown Inst: " + n);
	}
	
	// -----------------------------------------------------------------
	// Execution routines for individual Inst nodes
	// -----------------------------------------------------------------
	//
	// - Each execute() routine returns CONTINUE, RETURN, or a new idx
	// (target of jump).
	//
	
	// Binop ---
	// BOP op;
	// Dest dst;
	// Src src1, src2;
	//
	static int execute(IR1.Binop n) throws Exception
	{
		
		// ... code needed ...
		
		return CONTINUE;
	}
	
	// Unop ---
	// UOP op;
	// Dest dst;
	// Src src;
	//
	static int execute(IR1.Unop n) throws Exception
	{
		Val val = execute(n.src);
		Val res;
		if (n.op == IR1.UOP.NEG)
			res = new IntVal(-((IntVal) val).i);
		else if (n.op == IR0.UOP.NOT)
			res = new BoolVal(!((BoolVal) val).b);
		else
			throw new IntException("Wrong op in Unop inst: " + n.op);
		
		// ... code needed ...

		return CONTINUE;
	}
	
	// Move ---
	// Dest dst;
	// Src src;
	//
	static int execute(IR1.Move n) throws Exception
	{
		
		// ... code needed ...
		
	}
	
	// Load ---
	// Dest dst;
	// Addr addr;
	//
	static int execute(IR1.Load n) throws Exception
	{
		
		// ... code needed ...
		
	}
	
	// Store ---
	// Addr addr;
	// Src src;
	//
	static int execute(IR1.Store n) throws Exception
	{
		
		// ... code needed ...
		
	}
	
	// CJump ---
	// ROP op;
	// Src src1, src2;
	// Label lab;
	//
	static int execute(IR1.CJump n) throws Exception
	{
		
		// ... code needed ...
		
	}
	
	// Jump ---
	// Label lab;
	//
	static int execute(IR1.Jump n) throws Exception
	{
		
		// ... code needed ...
		
	}
	
	// Call ---
	// String name;
	// Src[] args;
	// Dest rdst;
	//
	static int execute(IR1.Call n) throws Exception
	{
		
		// ... code needed ...
		
	}
	
	// Return ---
	// Src val;
	//
	static int execute(IR1.Return n) throws Exception
	{
		
		// ... code needed ...
		
		return RETURN;
	}
	
	// -----------------------------------------------------------------
	// Evaluatation routines for address
	// -----------------------------------------------------------------
	//
	// - Returns an integer (representing index to the heap memory).
	//
	// Address ---
	// Src base;
	// int offset;
	//
	static int evalute(IR1.Addr n) throws Exception
	{
		
		// ... code needed ...
		
	}
	
	// -----------------------------------------------------------------
	// Evaluatation routines for operands
	// -----------------------------------------------------------------
	//
	// - Each evaluate() routine returns a Val object.
	//
	static Val evaluate(IR1.Src n) throws Exception
	{
		Val val;
		// if (n instanceof IR1.Temp) val =
		// if (n instanceof IR1.Id) val =
		// if (n instanceof IR1.IntLit) val =
		// if (n instanceof IR1.BoolLit) val =
		// if (n instanceof IR1.StrLit) val =
		return val;
	}
	
	static Val evaluate(IR1.Dest n) throws Exception
	{
		Val val;
		// if (n instanceof IR0.Temp) val =
		// if (n instanceof IR0.Id) val =
		return val;
	}
	
}
